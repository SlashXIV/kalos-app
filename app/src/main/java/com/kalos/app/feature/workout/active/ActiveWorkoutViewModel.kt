package com.kalos.app.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.data.ActiveWorkoutStore
import com.kalos.app.core.data.ExerciseDraft
import com.kalos.app.core.data.SetDraft
import com.kalos.app.core.data.WorkoutDraft
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.util.parseDurationToSecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SetInput(
    val reps: String = "",
    val weight: String = "",
    val duration: String = "",  // "mm:ss" — empty for REPS_WEIGHT exercises
    val isCompleted: Boolean = false,
)

data class ExerciseProgress(
    val templateExercise: TemplateExercise,
    val sets: List<SetInput>,
    val status: ExerciseStatus = ExerciseStatus.PLANNED,
    val originalExerciseName: String = "",
    val originalTemplateExercise: TemplateExercise? = null, // in-memory only, not persisted to draft
    val originalSets: List<SetInput> = emptyList(),         // in-memory only
)

data class ActiveWorkoutUiState(
    val templateName: String = "",
    val exercises: List<ExerciseProgress> = emptyList(),
    val currentExIndex: Int = 0,
    val elapsedSecs: Int = 0,
    val restSecsLeft: Int = 0,
    val isResting: Boolean = false,
    val isSaving: Boolean = false,
    val savedLogId: Long? = null,
    val isLoading: Boolean = true,
    val resumeAvailable: Boolean = false,
    val resumeStartedAt: Long = 0L,
    val resumeIsStale: Boolean = false,
    val exercisePickerExIndex: Int = -1,  // -1=closed, ADD_EXERCISE_SENTINEL=add, else=replace
    val exercisePickerMuscle: String = "",
    val confirmReplaceExIndex: Int = -1,
    val confirmReplaceExercise: Exercise? = null,
    val errorMessage: String? = null,
    // Historical load reference per exercise id (PR + last session), loaded lazily.
    val exerciseReferences: Map<Long, ExerciseReference> = emptyMap(),
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val store: ActiveWorkoutStore,
) : ViewModel() {

    companion object {
        const val ADD_EXERCISE_SENTINEL = Int.MAX_VALUE
    }

    private val _state = MutableStateFlow(ActiveWorkoutUiState())
    val state: StateFlow<ActiveWorkoutUiState> = _state

    private var timerJob: Job? = null
    private var restJob: Job? = null
    private var loadedTemplateId: Long = -2L
    private var workoutStartTime: Long = 0L
    private var restStartedAt: Long = 0L
    private var restDurationSecs: Int = 0

    init {
        viewModelScope.launch {
            _state
                .filter { !it.isLoading && !it.isSaving && it.savedLogId == null && !it.resumeAvailable }
                .debounce(400)
                .collect { s -> persistDraft(s) }
        }
        // Load the historical reference (PR + last session) for every exercise present,
        // re-running when the exercise set changes (handles add / replace mid-session).
        viewModelScope.launch {
            _state
                .map { s -> s.exercises.map { it.templateExercise.exercise.id }.toSet() }
                .distinctUntilChanged()
                .collect { ids ->
                    val missing = ids - _state.value.exerciseReferences.keys
                    if (missing.isEmpty()) return@collect
                    val loaded = missing.mapNotNull { id ->
                        workoutRepository.getExerciseReference(id)?.let { id to it }
                    }.toMap()
                    if (loaded.isNotEmpty()) {
                        _state.update { it.copy(exerciseReferences = it.exerciseReferences + loaded) }
                    }
                }
        }
    }

    fun loadTemplate(id: Long) {
        if (loadedTemplateId == id) return
        loadedTemplateId = id
        viewModelScope.launch {
            val draft = store.load()
            when {
                draft != null && draft.templateId == id -> {
                    val isStale = System.currentTimeMillis() - draft.startedAt > ActiveWorkoutStore.EXPIRY_MS
                    _state.update {
                        it.copy(
                            templateName = draft.templateName,
                            resumeAvailable = true,
                            resumeStartedAt = draft.startedAt,
                            resumeIsStale = isStale,
                            isLoading = false,
                        )
                    }
                }
                draft != null -> {
                    store.clear()
                    startFresh(id)
                }
                else -> startFresh(id)
            }
        }
    }

    fun resumeDraft() {
        val draft = store.load() ?: return
        workoutStartTime = draft.startedAt
        val exercises = draft.exercises.map { ed ->
            ExerciseProgress(
                templateExercise = TemplateExercise(
                    id = ed.templateExId,
                    templateId = loadedTemplateId,
                    exercise = Exercise(
                        id = ed.exerciseId,
                        name = ed.exerciseName,
                        primaryMuscle = ed.exercisePrimaryMuscle,
                    ),
                    orderIndex = ed.orderIndex,
                    defaultSets = ed.defaultSets,
                    defaultReps = ed.defaultReps,
                    defaultWeightKg = ed.defaultWeightKg,
                    restSeconds = ed.restSeconds,
                    notes = ed.notes,
                ),
                sets = ed.sets.map { SetInput(reps = it.reps, weight = it.weight, duration = it.duration, isCompleted = it.isCompleted) },
                status = runCatching { ExerciseStatus.valueOf(ed.status) }.getOrDefault(ExerciseStatus.PLANNED),
                originalExerciseName = ed.originalExerciseName,
                // originalTemplateExercise and originalSets not restored — undo unavailable after kill
            )
        }
        _state.update {
            it.copy(
                templateName = draft.templateName,
                exercises = exercises,
                currentExIndex = draft.currentExIndex,
                resumeAvailable = false,
                isLoading = false,
            )
        }
        if (draft.restStartedAt != null) {
            val elapsed = ((System.currentTimeMillis() - draft.restStartedAt) / 1000).toInt()
            val left = (draft.restDurationSecs - elapsed).coerceAtLeast(0)
            if (left > 0) {
                restStartedAt = draft.restStartedAt
                restDurationSecs = draft.restDurationSecs
                _state.update { it.copy(isResting = true, restSecsLeft = left) }
                launchRestLoop()
            }
        }
        startTimer()
    }

    fun discardDraftAndStart() {
        store.clear()
        viewModelScope.launch {
            _state.update { it.copy(resumeAvailable = false, isLoading = true) }
            startFresh(loadedTemplateId)
        }
    }

    private suspend fun startFresh(id: Long) {
        workoutStartTime = System.currentTimeMillis()
        if (id > 0) {
            val template = workoutRepository.getTemplate(id)
            if (template != null) {
                val exercises = template.exercises.map { te ->
                    ExerciseProgress(
                        templateExercise = te,
                        sets = List(te.defaultSets) {
                            SetInput(
                                reps = te.defaultReps.toString(),
                                weight = if (te.defaultWeightKg > 0f) te.defaultWeightKg.toString() else "",
                            )
                        },
                    )
                }
                _state.update { it.copy(templateName = template.name, exercises = exercises, isLoading = false) }
            } else {
                _state.update { it.copy(templateName = "Séance libre", isLoading = false) }
            }
        } else {
            _state.update { it.copy(templateName = "Séance libre", isLoading = false) }
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = ((System.currentTimeMillis() - workoutStartTime) / 1000).toInt()
                _state.update { it.copy(elapsedSecs = elapsed) }
            }
        }
    }

    fun startRest(seconds: Int = 90) {
        restJob?.cancel()
        restStartedAt = System.currentTimeMillis()
        restDurationSecs = seconds
        _state.update { it.copy(isResting = true, restSecsLeft = seconds) }
        launchRestLoop()
    }

    private fun launchRestLoop() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val elapsed = ((System.currentTimeMillis() - restStartedAt) / 1000).toInt()
                val left = (restDurationSecs - elapsed).coerceAtLeast(0)
                _state.update { it.copy(restSecsLeft = left, isResting = left > 0) }
                if (left == 0) break
            }
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _state.update { it.copy(isResting = false, restSecsLeft = 0) }
    }

    // ── Exercise management ──────────────────────────────────────────────────

    fun skipExercise(exIndex: Int) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ep = exercises.getOrNull(exIndex) ?: return@update s
            exercises[exIndex] = ep.copy(status = ExerciseStatus.SKIPPED, sets = emptyList())
            val nextIndex = if (exIndex < exercises.size - 1) exIndex + 1 else exIndex
            s.copy(exercises = exercises, currentExIndex = nextIndex)
        }
    }

    fun undoSkip(exIndex: Int) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ep = exercises.getOrNull(exIndex) ?: return@update s
            val te = ep.templateExercise
            val restoredSets = List(te.defaultSets) {
                SetInput(
                    reps = te.defaultReps.toString(),
                    weight = if (te.defaultWeightKg > 0f) te.defaultWeightKg.toString() else "",
                )
            }
            exercises[exIndex] = ep.copy(status = ExerciseStatus.PLANNED, sets = restoredSets)
            s.copy(exercises = exercises)
        }
    }

    fun openReplacePicker(exIndex: Int) {
        val muscle = _state.value.exercises.getOrNull(exIndex)
            ?.templateExercise?.exercise?.primaryMuscle ?: ""
        _state.update { it.copy(exercisePickerExIndex = exIndex, exercisePickerMuscle = muscle) }
    }

    fun openAddPicker() {
        _state.update { it.copy(exercisePickerExIndex = ADD_EXERCISE_SENTINEL, exercisePickerMuscle = "") }
    }

    fun dismissPicker() {
        _state.update { it.copy(exercisePickerExIndex = -1, exercisePickerMuscle = "") }
    }

    fun onExercisePicked(exercise: Exercise) {
        val s = _state.value
        val exIndex = s.exercisePickerExIndex
        _state.update { it.copy(exercisePickerExIndex = -1, exercisePickerMuscle = "") }

        if (exIndex == ADD_EXERCISE_SENTINEL) {
            confirmAddExercise(exercise)
            return
        }

        val ep = s.exercises.getOrNull(exIndex) ?: return
        val hasData = ep.sets.any { it.reps.isNotBlank() || it.weight.isNotBlank() || it.isCompleted }
        if (hasData) {
            _state.update { it.copy(confirmReplaceExIndex = exIndex, confirmReplaceExercise = exercise) }
        } else {
            doReplaceExercise(exIndex, exercise)
        }
    }

    fun confirmReplace() {
        val s = _state.value
        val exIndex = s.confirmReplaceExIndex
        val exercise = s.confirmReplaceExercise ?: return
        _state.update { it.copy(confirmReplaceExIndex = -1, confirmReplaceExercise = null) }
        doReplaceExercise(exIndex, exercise)
    }

    fun cancelReplace() {
        _state.update { it.copy(confirmReplaceExIndex = -1, confirmReplaceExercise = null) }
    }

    private fun doReplaceExercise(exIndex: Int, newExercise: Exercise) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ep = exercises.getOrNull(exIndex) ?: return@update s
            val newTe = ep.templateExercise.copy(exercise = newExercise, defaultWeightKg = 0f)
            val newSets = List(ep.templateExercise.defaultSets) {
                SetInput(reps = ep.templateExercise.defaultReps.toString(), weight = "")
            }
            exercises[exIndex] = ExerciseProgress(
                templateExercise = newTe,
                sets = newSets,
                status = ExerciseStatus.REPLACED,
                originalExerciseName = ep.originalExerciseName.ifBlank { ep.templateExercise.exercise.name },
                originalTemplateExercise = ep.originalTemplateExercise ?: ep.templateExercise,
                originalSets = ep.originalSets.ifEmpty { ep.sets },
            )
            s.copy(exercises = exercises)
        }
    }

    fun undoReplace(exIndex: Int) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ep = exercises.getOrNull(exIndex) ?: return@update s
            val origTe = ep.originalTemplateExercise ?: return@update s
            exercises[exIndex] = ep.copy(
                templateExercise = origTe,
                sets = ep.originalSets,
                status = ExerciseStatus.PLANNED,
                originalExerciseName = "",
                originalTemplateExercise = null,
                originalSets = emptyList(),
            )
            s.copy(exercises = exercises)
        }
    }

    private fun confirmAddExercise(exercise: Exercise) {
        _state.update { s ->
            val newTe = TemplateExercise(
                id = 0,
                templateId = loadedTemplateId,
                exercise = exercise,
                orderIndex = s.exercises.size,
                defaultSets = 3,
                defaultReps = 10,
                defaultWeightKg = 0f,
                restSeconds = 90,
                notes = "",
            )
            val newEp = ExerciseProgress(
                templateExercise = newTe,
                sets = List(3) { SetInput(reps = "10", weight = "") },
                status = ExerciseStatus.ADDED,
            )
            // Don't change currentExIndex here: ScrollableTabRow's SubcomposeLayout measures
            // tabPositions from the previous frame's tabs. Jumping the index in the same update
            // causes tabPositions[newIndex] to be called before the new tab has been laid out,
            // triggering IndexOutOfBoundsException. Navigation to the new tab happens via
            // LaunchedEffect in the Screen, after the first render with the updated tab list.
            s.copy(exercises = s.exercises + newEp)
        }
    }

    // ── Set editing ──────────────────────────────────────────────────────────

    fun onRepsChange(exIndex: Int, setIndex: Int, value: String) =
        updateSet(exIndex, setIndex) { it.copy(reps = value.filter { c -> c.isDigit() }) }

    fun onWeightChange(exIndex: Int, setIndex: Int, value: String) =
        updateSet(exIndex, setIndex) { it.copy(weight = value) }

    fun onDurationChange(exIndex: Int, setIndex: Int, value: String) =
        // Keep only digits and a single ':'. Tolerates intermediate states while typing.
        updateSet(exIndex, setIndex) {
            val cleaned = value.filter { c -> c.isDigit() || c == ':' }
            it.copy(duration = cleaned)
        }

    fun toggleSetCompleted(exIndex: Int, setIndex: Int) {
        val wasCompleted = _state.value.exercises.getOrNull(exIndex)
            ?.sets?.getOrNull(setIndex)?.isCompleted ?: return
        updateSet(exIndex, setIndex) { it.copy(isCompleted = !wasCompleted) }
        if (!wasCompleted) {
            val restSecs = _state.value.exercises[exIndex].templateExercise.restSeconds
            startRest(if (restSecs > 0) restSecs else 90)
        }
    }

    private fun updateSet(exIndex: Int, setIndex: Int, transform: (SetInput) -> SetInput) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ex = exercises.getOrNull(exIndex) ?: return@update s
            val sets = ex.sets.toMutableList()
            if (setIndex !in sets.indices) return@update s
            sets[setIndex] = transform(sets[setIndex])
            exercises[exIndex] = ex.copy(sets = sets)
            s.copy(exercises = exercises)
        }
    }

    fun addSet(exIndex: Int) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ex = exercises.getOrNull(exIndex) ?: return@update s
            val last = ex.sets.lastOrNull() ?: SetInput()
            exercises[exIndex] = ex.copy(sets = ex.sets + last.copy(isCompleted = false))
            s.copy(exercises = exercises)
        }
    }

    fun removeSet(exIndex: Int, setIndex: Int) {
        _state.update { s ->
            val exercises = s.exercises.toMutableList()
            val ex = exercises.getOrNull(exIndex) ?: return@update s
            if (ex.sets.size <= 1) return@update s
            exercises[exIndex] = ex.copy(sets = ex.sets.toMutableList().also { it.removeAt(setIndex) })
            s.copy(exercises = exercises)
        }
    }

    fun selectExercise(index: Int) = _state.update { it.copy(currentExIndex = index) }

    // ── Finish ───────────────────────────────────────────────────────────────

    fun finish() {
        timerJob?.cancel()
        restJob?.cancel()
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val logExercises = s.exercises.mapIndexed { i, ep ->
                    val sets = ep.sets.mapIndexed { setIdx, si ->
                        WorkoutSet(
                            id = 0, logExerciseId = 0,  // logExerciseId assigned inside the transaction
                            setNumber = setIdx + 1,
                            reps = si.reps.toIntOrNull() ?: 0,
                            weightKg = si.weight.toFloatOrNull() ?: 0f,
                            durationSecs = parseDurationToSecs(si.duration) ?: 0,
                            isCompleted = si.isCompleted,
                        )
                    }
                    LogExercise(
                        id = 0, logId = 0,
                        exercise = ep.templateExercise.exercise,
                        orderIndex = i,
                        sets = sets,
                        status = ep.status,
                        replacedExerciseName = ep.originalExerciseName,
                    )
                }
                workoutRepository.completeWorkout(
                    log = WorkoutLog(
                        id = 0,
                        templateId = if (loadedTemplateId > 0) loadedTemplateId else null,
                        templateName = s.templateName,
                        date = LocalDate.now().toString(),
                        startedAt = workoutStartTime,
                        exercises = logExercises,
                    ),
                    durationSecs = s.elapsedSecs.toLong(),
                )
            }
                .onSuccess { logId ->
                    // Only clear the draft after a confirmed write.
                    store.clear()
                    _state.update { it.copy(isSaving = false, savedLogId = logId) }
                }
                .onFailure { e ->
                    // Draft kept so the user can retry without losing input.
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = e.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Échec de l'enregistrement de la séance",
                        )
                    }
                }
        }
    }

    fun onErrorShown() = _state.update { it.copy(errorMessage = null) }

    // ── Draft persistence ────────────────────────────────────────────────────

    private fun persistDraft(s: ActiveWorkoutUiState) {
        if (loadedTemplateId == -2L || s.exercises.isEmpty()) return
        val exercises = s.exercises.map { ep ->
            ExerciseDraft(
                exerciseId = ep.templateExercise.exercise.id,
                exerciseName = ep.templateExercise.exercise.name,
                exercisePrimaryMuscle = ep.templateExercise.exercise.primaryMuscle,
                templateExId = ep.templateExercise.id,
                orderIndex = ep.templateExercise.orderIndex,
                defaultSets = ep.templateExercise.defaultSets,
                defaultReps = ep.templateExercise.defaultReps,
                defaultWeightKg = ep.templateExercise.defaultWeightKg,
                restSeconds = ep.templateExercise.restSeconds,
                notes = ep.templateExercise.notes,
                sets = ep.sets.map { SetDraft(reps = it.reps, weight = it.weight, duration = it.duration, isCompleted = it.isCompleted) },
                status = ep.status.name,
                originalExerciseName = ep.originalExerciseName,
            )
        }
        store.save(
            WorkoutDraft(
                templateId = loadedTemplateId,
                templateName = s.templateName,
                startedAt = workoutStartTime,
                currentExIndex = s.currentExIndex,
                restStartedAt = if (s.isResting) restStartedAt else null,
                restDurationSecs = if (s.isResting) restDurationSecs else 0,
                exercises = exercises,
            )
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        restJob?.cancel()
    }
}

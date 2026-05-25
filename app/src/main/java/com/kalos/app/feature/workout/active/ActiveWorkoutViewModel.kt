package com.kalos.app.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.data.ActiveWorkoutStore
import com.kalos.app.core.data.ExerciseDraft
import com.kalos.app.core.data.SetDraft
import com.kalos.app.core.data.WorkoutDraft
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.WorkoutRepository
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
                sets = ed.sets.map { SetInput(reps = it.reps, weight = it.weight, isCompleted = it.isCompleted) },
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
            val exercises = s.exercises + newEp
            s.copy(exercises = exercises, currentExIndex = exercises.size - 1)
        }
    }

    // ── Set editing ──────────────────────────────────────────────────────────

    fun onRepsChange(exIndex: Int, setIndex: Int, value: String) =
        updateSet(exIndex, setIndex) { it.copy(reps = value) }

    fun onWeightChange(exIndex: Int, setIndex: Int, value: String) =
        updateSet(exIndex, setIndex) { it.copy(weight = value) }

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
        store.clear()
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val logExercises = s.exercises.mapIndexed { i, ep ->
                LogExercise(
                    id = 0, logId = 0,
                    exercise = ep.templateExercise.exercise,
                    orderIndex = i,
                    sets = emptyList(),
                    status = ep.status,
                    replacedExerciseName = ep.originalExerciseName,
                )
            }
            val logId = workoutRepository.startLog(
                WorkoutLog(
                    id = 0,
                    templateId = if (loadedTemplateId > 0) loadedTemplateId else null,
                    templateName = s.templateName,
                    date = LocalDate.now().toString(),
                    startedAt = workoutStartTime,
                    exercises = logExercises,
                )
            )
            val savedLog = workoutRepository.getLog(logId)
            if (savedLog != null) {
                var totalVolume = 0f
                savedLog.exercises.forEachIndexed { exIdx, le ->
                    val ep = s.exercises.getOrNull(exIdx) ?: return@forEachIndexed
                    if (ep.status == ExerciseStatus.SKIPPED) return@forEachIndexed
                    ep.sets.forEachIndexed { setIdx, si ->
                        val reps = si.reps.toIntOrNull() ?: 0
                        val weight = si.weight.toFloatOrNull() ?: 0f
                        if (si.isCompleted) totalVolume += reps * weight
                        workoutRepository.upsertSet(
                            logId = logId,
                            exerciseId = le.exercise.id,
                            set = WorkoutSet(
                                id = 0, logExerciseId = le.id,
                                setNumber = setIdx + 1,
                                reps = reps, weightKg = weight,
                                isCompleted = si.isCompleted,
                            )
                        )
                    }
                }
                workoutRepository.finishLog(logId, s.elapsedSecs.toLong(), totalVolume)
            }
            _state.update { it.copy(isSaving = false, savedLogId = logId) }
        }
    }

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
                sets = ep.sets.map { SetDraft(reps = it.reps, weight = it.weight, isCompleted = it.isCompleted) },
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

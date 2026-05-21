package com.kalos.app.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveWorkoutUiState())
    val state: StateFlow<ActiveWorkoutUiState> = _state

    private var timerJob: Job? = null
    private var restJob: Job? = null
    private var loadedTemplateId: Long = -2L
    private var workoutStartTime: Long = 0L

    fun loadTemplate(id: Long) {
        if (loadedTemplateId == id) return
        loadedTemplateId = id
        workoutStartTime = System.currentTimeMillis()
        viewModelScope.launch {
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
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(elapsedSecs = it.elapsedSecs + 1) }
            }
        }
    }

    fun startRest(seconds: Int = 90) {
        restJob?.cancel()
        _state.update { it.copy(isResting = true, restSecsLeft = seconds) }
        restJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                left--
                _state.update { it.copy(restSecsLeft = left, isResting = left > 0) }
            }
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _state.update { it.copy(isResting = false, restSecsLeft = 0) }
    }

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

    fun finish() {
        timerJob?.cancel()
        restJob?.cancel()
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val logExercises = s.exercises.mapIndexed { i, ep ->
                LogExercise(
                    id = 0, logId = 0,
                    exercise = ep.templateExercise.exercise,
                    orderIndex = i,
                    sets = emptyList(),
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
                    s.exercises.getOrNull(exIdx)?.sets?.forEachIndexed { setIdx, si ->
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

    override fun onCleared() {
        timerJob?.cancel()
        restJob?.cancel()
    }
}

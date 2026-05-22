package com.kalos.app.feature.workout.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.model.TemplateExercise
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.domain.repository.ExerciseRepository
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuilderUiState(
    val name: String = "",
    val description: String = "",
    val estimatedDuration: String = "60",
    val exercises: List<TemplateExercise> = emptyList(),
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val isLoading: Boolean = false,
    val availablePrograms: List<TrainingProgram> = emptyList(),
    val selectedProgramId: Long? = null,
    val selectedDayOfWeek: Int? = null,
)

@HiltViewModel
class WorkoutBuilderViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state
    private var editingTemplateId: Long = -1
    private var originalLinkProgramId: Long? = null

    init {
        viewModelScope.launch {
            programRepository.getAll().collect { programs ->
                _state.update { it.copy(availablePrograms = programs) }
            }
        }
    }

    fun loadTemplate(id: Long) {
        if (id <= 0) return
        if (editingTemplateId == id) return  // already loaded — don't overwrite in-progress edits
        editingTemplateId = id
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            workoutRepository.getTemplate(id)?.let { t ->
                _state.update {
                    it.copy(
                        name = t.name,
                        description = t.description,
                        estimatedDuration = t.estimatedDurationMin.toString(),
                        exercises = t.exercises,
                        isLoading = false,
                    )
                }
            }
            val links = programRepository.getLinksForTemplate(id)
            val first = links.firstOrNull()
            if (first != null) {
                originalLinkProgramId = first.programId
                _state.update { it.copy(selectedProgramId = first.programId, selectedDayOfWeek = first.dayOfWeek) }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onDurationChange(v: String) = _state.update { it.copy(estimatedDuration = v) }

    fun onProgramSelected(programId: Long?) {
        _state.update { it.copy(selectedProgramId = programId, selectedDayOfWeek = null) }
    }

    fun onDaySelected(day: Int?) {
        _state.update { it.copy(selectedDayOfWeek = day) }
    }

    fun addExercise(exercise: Exercise) {
        _state.update { state ->
            val newEx = TemplateExercise(
                templateId = editingTemplateId,
                exercise = exercise,
                orderIndex = state.exercises.size,
            )
            state.copy(exercises = state.exercises + newEx)
        }
    }

    fun addExerciseById(id: Long) {
        viewModelScope.launch {
            exerciseRepository.getById(id)?.let { addExercise(it) }
        }
    }

    fun removeExercise(index: Int) {
        _state.update { state ->
            state.copy(exercises = state.exercises.toMutableList().also { it.removeAt(index) })
        }
    }

    fun updateExerciseParams(index: Int, sets: Int, reps: Int) {
        _state.update { state ->
            state.copy(exercises = state.exercises.mapIndexed { i, te ->
                if (i == index) te.copy(defaultSets = sets, defaultReps = reps) else te
            })
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val id = workoutRepository.saveTemplate(
                WorkoutTemplate(
                    id = if (editingTemplateId > 0) editingTemplateId else 0,
                    name = s.name.trim(),
                    description = s.description.trim(),
                    estimatedDurationMin = s.estimatedDuration.toIntOrNull() ?: 60,
                    exercises = s.exercises,
                )
            )
            val newProgramId = s.selectedProgramId
            val newDay = s.selectedDayOfWeek
            if (newProgramId != null && newDay != null) {
                programRepository.linkTemplate(id, newProgramId, newDay)
            } else if (originalLinkProgramId != null) {
                programRepository.unlinkTemplate(id, originalLinkProgramId!!)
            }
            _state.update { it.copy(isSaving = false, savedId = id) }
        }
    }
}

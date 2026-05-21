package com.kalos.app.feature.workout.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.model.TemplateExercise
import com.kalos.app.core.domain.model.WorkoutTemplate
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
)

@HiltViewModel
class WorkoutBuilderViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state
    private var editingTemplateId: Long = -1

    fun loadTemplate(id: Long) {
        if (id <= 0) return
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
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onDurationChange(v: String) = _state.update { it.copy(estimatedDuration = v) }

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

    fun removeExercise(index: Int) {
        _state.update { state ->
            state.copy(exercises = state.exercises.toMutableList().also { it.removeAt(index) })
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
            _state.update { it.copy(isSaving = false, savedId = id) }
        }
    }
}

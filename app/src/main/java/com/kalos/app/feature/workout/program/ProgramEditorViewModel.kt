package com.kalos.app.feature.workout.program

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramEditorState(
    val name: String = "",
    val description: String = "",
    val durationWeeks: Int = 4,
    val assignments: Map<Int, WorkoutTemplate?> = (1..7).associateWith { null },
    val templates: List<WorkoutTemplate> = emptyList(),
    val isCustom: Boolean = true,
    val isSaved: Boolean = false,
)

@HiltViewModel
class ProgramEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val programId: Long = savedStateHandle["programId"] ?: -1L

    private val _state = MutableStateFlow(ProgramEditorState())
    val state: StateFlow<ProgramEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.getTemplates().collect { templates ->
                _state.update { it.copy(templates = templates) }
            }
        }
        if (programId > 0L) {
            viewModelScope.launch {
                programRepository.getById(programId)?.let { program ->
                    _state.update {
                        it.copy(
                            name = program.name,
                            description = program.description,
                            durationWeeks = program.durationWeeks,
                            isCustom = program.isCustom,
                            assignments = (1..7).associateWith { day ->
                                program.workouts.firstOrNull { pw -> pw.dayOfWeek == day }?.template
                            },
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setDurationWeeks(v: Int) = _state.update { it.copy(durationWeeks = v.coerceIn(1, 52)) }

    fun assign(day: Int, template: WorkoutTemplate?) = _state.update {
        it.copy(assignments = it.assignments.toMutableMap().also { m -> m[day] = template })
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val workouts = s.assignments.entries
                .filter { it.value != null }
                .map { (day, template) ->
                    ProgramWorkout(
                        programId = if (programId > 0L) programId else 0L,
                        template = template,
                        dayOfWeek = day,
                    )
                }
            programRepository.save(
                TrainingProgram(
                    id = if (programId > 0L) programId else 0L,
                    name = s.name.trim(),
                    description = s.description.trim(),
                    durationWeeks = s.durationWeeks,
                    daysPerWeek = workouts.size,
                    isCustom = true,
                    workouts = workouts,
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (programId <= 0L) return
        viewModelScope.launch {
            val s = _state.value
            programRepository.delete(TrainingProgram(id = programId, name = s.name, isCustom = true))
            _state.update { it.copy(isSaved = true) }
        }
    }
}

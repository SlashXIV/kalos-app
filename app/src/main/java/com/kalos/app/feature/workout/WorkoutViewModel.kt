package com.kalos.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<WorkoutUiState> = workoutRepository.getTemplates()
        .map { templates -> WorkoutUiState(templates = templates, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutUiState(),
        )

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { workoutRepository.deleteTemplate(template) }
    }
}

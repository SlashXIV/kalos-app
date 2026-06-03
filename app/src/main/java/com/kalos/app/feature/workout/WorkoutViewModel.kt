package com.kalos.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.data.ActiveWorkoutStore
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Minimal banner state for an in-progress workout, surfaced on the Sport screen. */
data class DraftBannerState(
    val templateId: Long,
    val templateName: String,
    val exerciseCount: Int,
    val startedAt: Long,
)

data class WorkoutUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val draftBanner: DraftBannerState? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val activeWorkoutStore: ActiveWorkoutStore,
) : ViewModel() {

    val uiState: StateFlow<WorkoutUiState> = combine(
        workoutRepository.getTemplates(),
        activeWorkoutStore.draftFlow,
    ) { templates, draft ->
        WorkoutUiState(
            templates = templates,
            draftBanner = draft?.let {
                DraftBannerState(
                    templateId = it.templateId,
                    templateName = it.templateName,
                    exerciseCount = it.exercises.size,
                    startedAt = it.startedAt,
                )
            },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState(),
    )

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { workoutRepository.deleteTemplate(template) }
    }
}

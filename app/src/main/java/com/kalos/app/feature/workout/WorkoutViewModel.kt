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
    // Last activity timestamp (last auto-save; falls back to session start for legacy
    // drafts). Drives both the "il y a X" label and the staleness flag.
    val lastActiveAt: Long,
    // Draft inactive longer than ActiveWorkoutStore.EXPIRY_MS (24h) — almost certainly
    // abandoned. The banner switches to a neutral "Séance interrompue" tone and offers discarding.
    val isStale: Boolean,
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
                // Both the elapsed label and the staleness flag are measured from the last
                // activity (auto-save), falling back to session start for legacy drafts.
                val lastActive = if (it.lastSavedAt > 0) it.lastSavedAt else it.startedAt
                DraftBannerState(
                    templateId = it.templateId,
                    templateName = it.templateName,
                    exerciseCount = it.exercises.size,
                    lastActiveAt = lastActive,
                    isStale = System.currentTimeMillis() - lastActive > ActiveWorkoutStore.EXPIRY_MS,
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

    /** Discards the in-progress draft. The reactive draftFlow removes the banner automatically. */
    fun discardDraft() = activeWorkoutStore.clear()
}

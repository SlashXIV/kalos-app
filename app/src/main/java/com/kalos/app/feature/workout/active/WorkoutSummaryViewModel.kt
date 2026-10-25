package com.kalos.app.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.R
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.annotation.StringRes
import android.util.Log

data class SummaryPendingEdit(
    val set: WorkoutSet,
    val exerciseId: Long,
    val trackingMode: ExerciseTrackingMode,
)

data class WorkoutSummaryUiState(
    val log: WorkoutLog? = null,
    val pendingEdit: SummaryPendingEdit? = null,
    val isSavingEdit: Boolean = false,
    @StringRes val errorMessage: Int? = null,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState

    fun loadLog(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(log = workoutRepository.getLog(id)) }
        }
    }

    fun openEditDialog(set: WorkoutSet, exerciseId: Long, trackingMode: ExerciseTrackingMode) {
        _uiState.update { it.copy(pendingEdit = SummaryPendingEdit(set, exerciseId, trackingMode)) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(pendingEdit = null) }
    }

    fun saveSetEdit(newReps: Int, newWeightKg: Float, newDurationSecs: Int) {
        val pending = _uiState.value.pendingEdit ?: return
        val log = _uiState.value.log ?: return
        _uiState.update { it.copy(pendingEdit = null, isSavingEdit = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                workoutRepository.editSet(
                    logId = log.id,
                    exerciseId = pending.exerciseId,
                    set = pending.set.copy(
                        reps = newReps,
                        weightKg = newWeightKg,
                        durationSecs = newDurationSecs,
                    ),
                )
            }
                .onSuccess { finalLog ->
                    _uiState.update { it.copy(log = finalLog, isSavingEdit = false) }
                }
                .onFailure { e ->
                    Log.w("Kalos", "Operation failed", e)
                    _uiState.update {
                        it.copy(
                            isSavingEdit = false,
                            errorMessage = R.string.error_set_edit_failed,
                        )
                    }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }
}

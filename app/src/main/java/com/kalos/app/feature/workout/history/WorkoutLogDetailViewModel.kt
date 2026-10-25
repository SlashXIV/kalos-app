package com.kalos.app.feature.workout.history

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.annotation.StringRes
import android.util.Log

data class PendingSetEdit(
    val set: WorkoutSet,
    val exerciseId: Long,
    val trackingMode: ExerciseTrackingMode,
)

data class WorkoutLogDetailUiState(
    val log: WorkoutLog? = null,
    val maxWeights: Map<Long, Float?> = emptyMap(),
    val isLoading: Boolean = true,
    val pendingEdit: PendingSetEdit? = null,
    val isSavingEdit: Boolean = false,
    @StringRes val errorMessage: Int? = null,
)

@HiltViewModel
class WorkoutLogDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutLogDetailUiState())
    val uiState: StateFlow<WorkoutLogDetailUiState> = _uiState.asStateFlow()

    fun load(logId: Long) {
        viewModelScope.launch {
            val log = workoutRepository.getLog(logId)
            if (log != null) {
                val maxWeights = workoutRepository.getMaxWeightsForExercises(
                    log.exercises.map { it.exercise.id }
                )
                _uiState.value = WorkoutLogDetailUiState(
                    log = log,
                    maxWeights = maxWeights,
                    isLoading = false,
                )
            } else {
                _uiState.value = WorkoutLogDetailUiState(isLoading = false)
            }
        }
    }

    fun openEditDialog(set: WorkoutSet, exerciseId: Long, trackingMode: ExerciseTrackingMode) {
        _uiState.update { it.copy(pendingEdit = PendingSetEdit(set, exerciseId, trackingMode)) }
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
                val finalLog = workoutRepository.editSet(
                    logId = log.id,
                    exerciseId = pending.exerciseId,
                    set = pending.set.copy(
                        reps = newReps,
                        weightKg = newWeightKg,
                        durationSecs = newDurationSecs,
                    ),
                )
                val maxWeights = finalLog?.let {
                    workoutRepository.getMaxWeightsForExercises(it.exercises.map { le -> le.exercise.id })
                } ?: emptyMap()
                finalLog to maxWeights
            }
                .onSuccess { (finalLog, maxWeights) ->
                    _uiState.update {
                        it.copy(log = finalLog, maxWeights = maxWeights, isSavingEdit = false)
                    }
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

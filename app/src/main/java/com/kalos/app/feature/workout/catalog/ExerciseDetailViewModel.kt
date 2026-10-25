package com.kalos.app.feature.workout.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.RepRecord
import com.kalos.app.core.domain.model.meaningful
import com.kalos.app.core.domain.usecase.EstimateOneRepMaxUseCase
import com.kalos.app.core.domain.repository.ExerciseRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val progression: List<Pair<String, Float>> = emptyList(),
    val pr: Float? = null,
    /** Records par répétitions, seulement ceux qu'aucune série plus longue n'a égalés. */
    val repRecords: List<RepRecord> = emptyList(),
    val oneRepMax: Float? = null,
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    private val estimateOneRepMax: EstimateOneRepMaxUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state

    fun load(id: Long) {
        viewModelScope.launch {
            val exercise = repo.getById(id)
            val progression = workoutRepo.getExerciseProgression(id)
            val pr = workoutRepo.getMaxWeight(id)
            // Répétitions × charge seulement : en durée, « reps » n'a pas de sens.
            val records = if (exercise?.trackingMode == ExerciseTrackingMode.REPS_WEIGHT) workoutRepo.getRepRecords(id) else emptyList()
            _state.value = ExerciseDetailUiState(
                exercise = exercise, progression = progression, pr = pr,
                repRecords = records.meaningful(), oneRepMax = estimateOneRepMax(records),
            )
        }
    }
}

package com.kalos.app.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.ActivityType
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.domain.usecase.EstimateActivityEnergyUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogActivityViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    userRepository: UserRepository,
    private val estimateEnergy: EstimateActivityEnergyUseCase,
) : ViewModel() {

    val weight: StateFlow<Float?> = userRepository.observeProfile()
        .map { it?.weightKg }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Estimated energy, or null when the type or the profile weight is unknown. */
    fun estimate(type: ActivityType?, weightKg: Float?, durationSecs: Long): Int? =
        if (type == null || weightKg == null || durationSecs <= 0) null else estimateEnergy(type, weightKg, durationSecs)

    /** Records a free activity (e.g. climbing 2h) as a lightweight session with no exercises. */
    fun logActivity(name: String, type: ActivityType?, durationSecs: Long, date: String, note: String) {
        val log = WorkoutLog(
            templateName = name.trim(),
            date = date,
            startedAt = System.currentTimeMillis(),
            notes = note.trim(),
            exercises = emptyList(),
            activityType = type,
            energyKcal = estimate(type, weight.value, durationSecs),
        )
        viewModelScope.launch { workoutRepository.completeWorkout(log, durationSecs) }
    }
}

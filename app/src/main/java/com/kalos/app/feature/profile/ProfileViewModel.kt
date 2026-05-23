package com.kalos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val goal: NutritionGoal? = null,
    val tdee: Float = 0f,
    val lastWeightKg: Float? = null,
    val lastWeightDate: String? = null,
    val weightDelta: Float? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val calculateTdee: CalculateTdeeUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.observeProfile(),
        userRepository.observeGoal(),
        workoutRepository.getBodyWeightHistory(),
    ) { profile, goal, history ->
        val last = history.firstOrNull()
        val prev = history.getOrNull(1)
        val tdee = if (profile != null) calculateTdee(profile) else 0f
        ProfileUiState(
            profile = profile,
            goal = goal,
            tdee = tdee,
            lastWeightKg = last?.second,
            lastWeightDate = last?.first,
            weightDelta = if (last != null && prev != null) last.second - prev.second else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}

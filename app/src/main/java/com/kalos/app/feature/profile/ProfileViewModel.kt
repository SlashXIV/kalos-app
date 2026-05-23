package com.kalos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val goal: NutritionGoal? = null,
    val tdee: Float = 0f,
    val lastWeightKg: Float? = null,
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
        workoutRepository.getBodyWeightHistory().map { it.firstOrNull()?.second },
    ) { profile, goal, lastWeightKg ->
        val tdee = if (profile != null) calculateTdee(profile) else 0f
        ProfileUiState(profile = profile, goal = goal, tdee = tdee, lastWeightKg = lastWeightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}

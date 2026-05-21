package com.kalos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val goal: NutritionGoal? = null,
    val tdee: Float = 0f,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateTdee: CalculateTdeeUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.observeProfile(),
        userRepository.observeGoal(),
    ) { profile, goal ->
        val tdee = if (profile != null) calculateTdee(profile) else 0f
        ProfileUiState(profile = profile, goal = goal, tdee = tdee)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}

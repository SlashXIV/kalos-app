package com.kalos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
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

    /**
     * Aligns the reference weight (TDEE basis) with the most recent body-weight log entry.
     * Mirrors EditProfileViewModel's resync rule: nutrition goals are recomputed only when
     * they are not custom — hand-tuned macros are never overwritten.
     */
    fun updateReferenceWeightFromLog() {
        val s = uiState.value
        val profile = s.profile ?: return
        val lastWeight = s.lastWeightKg ?: return
        viewModelScope.launch {
            val updated = profile.copy(weightKg = lastWeight)
            userRepository.saveProfile(updated)
            val existingGoal = userRepository.getGoal()
            if (existingGoal == null || !existingGoal.isCustom) {
                userRepository.saveGoal(calculateMacroGoals(updated))
            }
        }
    }
}

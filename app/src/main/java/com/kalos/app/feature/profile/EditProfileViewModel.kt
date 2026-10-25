package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kalos.app.core.ui.util.parseDecimal

data class EditProfileState(
    val name: String = "",
    val age: String = "25",
    val sex: Sex = Sex.MALE,
    val heightCm: String = "175",
    val weightKg: String = "75",
    val targetWeightKg: String = "70",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: FitnessGoal = FitnessGoal.MAINTAIN,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state

    private var originalFitnessGoal: FitnessGoal? = null

    init {
        viewModelScope.launch {
            userRepository.getProfile()?.let { p ->
                originalFitnessGoal = p.goal
                _state.update {
                    it.copy(
                        name = p.name, age = p.age.toString(), sex = p.sex,
                        heightCm = p.heightCm.toString(), weightKg = p.weightKg.toString(),
                        targetWeightKg = p.targetWeightKg.toString(),
                        activityLevel = p.activityLevel, goal = p.goal,
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v) }
    fun onSexChange(v: Sex) = _state.update { it.copy(sex = v) }
    fun onHeightChange(v: String) = _state.update { it.copy(heightCm = v) }
    fun onWeightChange(v: String) = _state.update { it.copy(weightKg = v) }
    fun onTargetWeightChange(v: String) = _state.update { it.copy(targetWeightKg = v) }
    fun onActivityChange(v: ActivityLevel) = _state.update { it.copy(activityLevel = v) }
    fun onGoalChange(v: FitnessGoal) = _state.update { it.copy(goal = v) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val profile = UserProfile(
                name = s.name.trim(), age = s.age.toIntOrNull() ?: 25,
                sex = s.sex, heightCm = s.heightCm.parseDecimal() ?: 175f,
                weightKg = s.weightKg.parseDecimal() ?: 75f,
                targetWeightKg = s.targetWeightKg.parseDecimal() ?: 70f,
                activityLevel = s.activityLevel, goal = s.goal,
                onboardingCompleted = true,
            )
            userRepository.saveProfile(profile)

            // Resync nutrition goal when the fitness objective or activity level changed,
            // or when no custom goal exists yet.
            val fitnessGoalChanged = s.goal != originalFitnessGoal
            val existingGoal = userRepository.getGoal()
            if (fitnessGoalChanged || existingGoal == null || !existingGoal.isCustom) {
                userRepository.saveGoal(calculateMacroGoals(profile))
            }

            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

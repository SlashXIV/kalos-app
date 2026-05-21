package com.kalos.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val name: String = "",
    val age: String = "25",
    val sex: Sex = Sex.MALE,
    val heightCm: String = "175",
    val weightKg: String = "75",
    val targetWeightKg: String = "70",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: FitnessGoal = FitnessGoal.MAINTAIN,
    val calculatedKcal: Int = 0,
    val calculatedProtein: Int = 0,
    val calculatedCarbs: Int = 0,
    val calculatedFat: Int = 0,
    val bmr: Float = 0f,
    val tdee: Float = 0f,
    val isSaving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateBmr: CalculateBmrUseCase,
    private val calculateTdee: CalculateTdeeUseCase,
    private val calculateMacros: CalculateMacroGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v) }
    fun onSexChange(v: Sex) = _state.update { it.copy(sex = v) }
    fun onHeightChange(v: String) = _state.update { it.copy(heightCm = v) }
    fun onWeightChange(v: String) = _state.update { it.copy(weightKg = v) }
    fun onTargetWeightChange(v: String) = _state.update { it.copy(targetWeightKg = v) }
    fun onActivityChange(v: ActivityLevel) = _state.update { it.copy(activityLevel = v) }
    fun onGoalChange(v: FitnessGoal) = _state.update { it.copy(goal = v) }

    fun calculateResults() {
        val s = _state.value
        val profile = buildProfile(s)
        val macros = calculateMacros(profile)
        val bmr = calculateBmr(profile)
        val tdee = calculateTdee(profile)
        _state.update {
            it.copy(
                calculatedKcal = macros.kcal,
                calculatedProtein = macros.proteinG,
                calculatedCarbs = macros.carbsG,
                calculatedFat = macros.fatG,
                bmr = bmr,
                tdee = tdee,
            )
        }
    }

    fun saveAndComplete(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val s = _state.value
            val profile = buildProfile(s).copy(onboardingCompleted = true)
            val macros = calculateMacros(profile)
            userRepository.saveProfile(profile)
            userRepository.saveGoal(macros)
            _state.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    private fun buildProfile(s: OnboardingState) = UserProfile(
        name = s.name.trim(),
        age = s.age.toIntOrNull() ?: 25,
        sex = s.sex,
        heightCm = s.heightCm.toFloatOrNull() ?: 175f,
        weightKg = s.weightKg.toFloatOrNull() ?: 75f,
        targetWeightKg = s.targetWeightKg.toFloatOrNull() ?: 70f,
        activityLevel = s.activityLevel,
        goal = s.goal,
    )
}

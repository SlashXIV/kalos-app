package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditGoalsState(
    val kcal: String = NutritionGoal.DEFAULT_KCAL.toString(),
    val protein: String = NutritionGoal.DEFAULT_PROTEIN_G.toString(),
    val carbs: String = NutritionGoal.DEFAULT_CARBS_G.toString(),
    val fat: String = NutritionGoal.DEFAULT_FAT_G.toString(),
    val hasProfile: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    // Breakdown from auto-calc
    val showBreakdown: Boolean = false,
    val bmr: Float = 0f,
    val tdee: Float = 0f,
    val goalDelta: Int = 0,
    val activityLabel: String = "",
    val goalLabel: String = "",
    // Macro breakdown detail
    val bWeightKg: Float = 0f,
    val bProteinPerKg: Float = 0f,
    val bFatPerKg: Float = 0f,
    val bProteinG: Int = 0,
    val bFatG: Int = 0,
    val bCarbsG: Int = 0,
    val bTargetKcal: Int = 0,
) {
    val isValid: Boolean get() = (kcal.toIntOrNull() ?: 0) > 0 &&
        listOf(protein, carbs, fat).all { (it.toIntOrNull() ?: -1) >= 0 }
}

@HiltViewModel
class EditGoalsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
    private val calculateTdee: CalculateTdeeUseCase,
    private val calculateBmr: CalculateBmrUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditGoalsState())
    val state: StateFlow<EditGoalsState> = _state

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            userRepository.getGoal()?.let { g ->
                _state.update {
                    it.copy(
                        kcal = g.kcal.toString(), protein = g.proteinG.toString(),
                        carbs = g.carbsG.toString(), fat = g.fatG.toString(),
                        hasProfile = profile != null,
                    )
                }
            } ?: _state.update { it.copy(hasProfile = profile != null) }
        }
    }

    fun onKcalChange(v: String) = _state.update { it.copy(kcal = v) }
    fun onProteinChange(v: String) = _state.update { it.copy(protein = v) }
    fun onCarbsChange(v: String) = _state.update { it.copy(carbs = v) }
    fun onFatChange(v: String) = _state.update { it.copy(fat = v) }

    fun autoCalculate() {
        viewModelScope.launch {
            val profile = userRepository.getProfile() ?: return@launch
            val bmr = calculateBmr(profile)
            val tdee = calculateTdee(profile)
            val goal = calculateMacroGoals(profile)
            _state.update {
                it.copy(
                    kcal = goal.kcal.toString(),
                    protein = goal.proteinG.toString(),
                    carbs = goal.carbsG.toString(),
                    fat = goal.fatG.toString(),
                    showBreakdown = true,
                    bmr = bmr,
                    tdee = tdee,
                    goalDelta = profile.goal.kcalDelta,
                    activityLabel = profile.activityLevel.label,
                    goalLabel = profile.goal.label,
                    bWeightKg = profile.weightKg,
                    bProteinPerKg = profile.goal.proteinPerKg,
                    bFatPerKg = profile.goal.fatPerKg,
                    bProteinG = goal.proteinG,
                    bFatG = goal.fatG,
                    bCarbsG = goal.carbsG,
                    bTargetKcal = goal.kcal,
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        // An empty or invalid field used to silently save the default value: refuse instead.
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            userRepository.saveGoal(
                NutritionGoal(kcal = s.kcal.toInt(), proteinG = s.protein.toInt(), carbsG = s.carbs.toInt(), fatG = s.fat.toInt(), isCustom = true)
            )
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

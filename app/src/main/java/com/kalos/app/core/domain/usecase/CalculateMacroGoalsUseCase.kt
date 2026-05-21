package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import javax.inject.Inject
import kotlin.math.roundToInt

class CalculateMacroGoalsUseCase @Inject constructor(
    private val calculateTdee: CalculateTdeeUseCase,
) {
    operator fun invoke(profile: UserProfile): NutritionGoal {
        val tdee = calculateTdee(profile)
        val targetKcal = (tdee + profile.goal.kcalDelta).coerceAtLeast(1200f)

        // Protein: 2g per kg body weight (fitness standard)
        val proteinG = (profile.weightKg * 2f).roundToInt()
        val proteinKcal = proteinG * 4f

        // Fat: 25% of target calories
        val fatKcal = targetKcal * 0.25f
        val fatG = (fatKcal / 9f).roundToInt()

        // Carbs: remaining calories
        val carbsKcal = targetKcal - proteinKcal - fatKcal
        val carbsG = (carbsKcal / 4f).coerceAtLeast(0f).roundToInt()

        return NutritionGoal(
            kcal = targetKcal.roundToInt(),
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            isCustom = false,
        )
    }
}

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

        // Protein: goal-specific g/kg (higher during cuts to spare muscle)
        val proteinG = (profile.weightKg * profile.goal.proteinPerKg).roundToInt()
        val proteinKcal = proteinG * 4f

        // Fat: goal-specific g/kg (lower during aggressive cuts)
        val fatG = (profile.weightKg * profile.goal.fatPerKg).roundToInt()
        val fatKcal = fatG * 9f

        // Carbs: remaining calories after protein and fat
        val carbsKcal = (targetKcal - proteinKcal - fatKcal).coerceAtLeast(0f)
        val carbsG = (carbsKcal / 4f).roundToInt()

        return NutritionGoal(
            kcal = targetKcal.roundToInt(),
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            isCustom = false,
        )
    }
}

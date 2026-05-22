package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.model.NutritionGoal
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class FoodSuggestion(
    val food: Food,
    val servingG: Float,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)

class SuggestFoodsUseCase @Inject constructor() {

    operator fun invoke(
        foods: List<Food>,
        goal: NutritionGoal,
        consumedKcal: Float,
        consumedProtein: Float,
        consumedCarbs: Float,
        consumedFat: Float,
    ): List<FoodSuggestion> {
        val remKcal = max(0f, goal.kcal - consumedKcal)
        val remProtein = max(0f, goal.proteinG - consumedProtein)
        val remCarbs = max(0f, goal.carbsG - consumedCarbs)
        val remFat = max(0f, goal.fatG - consumedFat)

        if (remKcal < 50f) return emptyList()

        val remMacroKcal = remProtein * 4f + remCarbs * 4f + remFat * 9f
        if (remMacroKcal < 1f) return emptyList()

        // Macro weights = proportional to their remaining kcal contribution
        val wProtein = remProtein * 4f / remMacroKcal
        val wCarbs = remCarbs * 4f / remMacroKcal
        val wFat = remFat * 9f / remMacroKcal

        return foods
            .map { food ->
                val serving = food.defaultServingG
                val fKcal = food.kcalForAmount(serving)
                val fProtein = food.proteinForAmount(serving)
                val fCarbs = food.carbsForAmount(serving)
                val fFat = food.fatForAmount(serving)

                // Coverage: how much of the remaining need does this serving fill (capped at 100%)
                val covProtein = min(fProtein / max(remProtein, 0.1f), 1f)
                val covCarbs = min(fCarbs / max(remCarbs, 0.1f), 1f)
                val covFat = min(fFat / max(remFat, 0.1f), 1f)

                var score = covProtein * wProtein + covCarbs * wCarbs + covFat * wFat
                // Heavily penalize foods that would overshoot remaining calories
                if (fKcal > remKcal * 1.1f) score *= 0.2f

                FoodSuggestion(
                    food = food,
                    servingG = serving,
                    kcal = fKcal,
                    proteinG = fProtein,
                    carbsG = fCarbs,
                    fatG = fFat,
                ) to score
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
}

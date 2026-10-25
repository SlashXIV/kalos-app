package com.kalos.app.core.domain.model

data class DailyNutritionSummary(
    val date: String,
    val totalKcal: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val goalKcal: Int = NutritionGoal.DEFAULT_KCAL,
    val goalProtein: Int = NutritionGoal.DEFAULT_PROTEIN_G,
    val goalCarbs: Int = NutritionGoal.DEFAULT_CARBS_G,
    val goalFat: Int = NutritionGoal.DEFAULT_FAT_G,
    val meals: List<MealEntry> = emptyList(),
) {
    val remainingKcal get() = goalKcal - totalKcal
    val kcalProgress get() = if (goalKcal > 0) (totalKcal / goalKcal).coerceIn(0f, 1f) else 0f
    val proteinProgress get() = if (goalProtein > 0) (totalProtein / goalProtein).coerceIn(0f, 1f) else 0f
    val carbsProgress get() = if (goalCarbs > 0) (totalCarbs / goalCarbs).coerceIn(0f, 1f) else 0f
    val fatProgress get() = if (goalFat > 0) (totalFat / goalFat).coerceIn(0f, 1f) else 0f
}

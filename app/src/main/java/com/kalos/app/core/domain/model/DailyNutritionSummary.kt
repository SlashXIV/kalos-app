package com.kalos.app.core.domain.model

data class DailyNutritionSummary(
    val date: String,
    val totalKcal: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val goalKcal: Int = 2000,
    val goalProtein: Int = 150,
    val goalCarbs: Int = 200,
    val goalFat: Int = 67,
    val meals: List<MealEntry> = emptyList(),
) {
    val remainingKcal get() = goalKcal - totalKcal
    val kcalProgress get() = if (goalKcal > 0) (totalKcal / goalKcal).coerceIn(0f, 1f) else 0f
    val proteinProgress get() = if (goalProtein > 0) (totalProtein / goalProtein).coerceIn(0f, 1f) else 0f
    val carbsProgress get() = if (goalCarbs > 0) (totalCarbs / goalCarbs).coerceIn(0f, 1f) else 0f
    val fatProgress get() = if (goalFat > 0) (totalFat / goalFat).coerceIn(0f, 1f) else 0f
}

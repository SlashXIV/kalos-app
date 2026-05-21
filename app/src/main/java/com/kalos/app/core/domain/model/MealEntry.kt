package com.kalos.app.core.domain.model

data class MealEntry(
    val id: Long = 0,
    val date: String,
    val mealType: MealType,
    val items: List<MealItem> = emptyList(),
) {
    val totalKcal get() = items.sumOf { it.kcal.toDouble() }.toFloat()
    val totalProtein get() = items.sumOf { it.proteinG.toDouble() }.toFloat()
    val totalCarbs get() = items.sumOf { it.carbsG.toDouble() }.toFloat()
    val totalFat get() = items.sumOf { it.fatG.toDouble() }.toFloat()
}

data class MealItem(
    val id: Long = 0,
    val mealEntryId: Long,
    val food: Food,
    val amountG: Float,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)

enum class MealType(val label: String) {
    BREAKFAST("Petit-déjeuner"),
    LUNCH("Déjeuner"),
    DINNER("Dîner"),
    SNACK("Collation"),
}

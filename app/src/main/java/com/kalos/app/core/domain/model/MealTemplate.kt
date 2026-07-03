package com.kalos.app.core.domain.model

/** A reusable "favourite meal" — a named set of foods with amounts, applied to a meal in one tap. */
data class MealTemplate(
    val id: Long = 0,
    val name: String,
    val items: List<MealTemplateItem> = emptyList(),
) {
    val totalKcal: Float get() = items.sumOf { it.kcal.toDouble() }.toFloat()
}

data class MealTemplateItem(
    val food: Food,
    val amountG: Float,
) {
    val kcal: Float get() = food.kcalForAmount(amountG)
    val proteinG: Float get() = food.proteinForAmount(amountG)
    val carbsG: Float get() = food.carbsForAmount(amountG)
    val fatG: Float get() = food.fatForAmount(amountG)
}

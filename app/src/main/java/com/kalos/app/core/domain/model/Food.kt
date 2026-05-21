package com.kalos.app.core.domain.model

data class Food(
    val id: Long = 0,
    val name: String,
    val brand: String = "",
    val category: String = "Divers",
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val defaultServingG: Float = 100f,
    val servingUnit: String = "g",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0,
) {
    fun kcalForAmount(amountG: Float) = kcalPer100g * amountG / 100f
    fun proteinForAmount(amountG: Float) = proteinPer100g * amountG / 100f
    fun carbsForAmount(amountG: Float) = carbsPer100g * amountG / 100f
    fun fatForAmount(amountG: Float) = fatPer100g * amountG / 100f
}

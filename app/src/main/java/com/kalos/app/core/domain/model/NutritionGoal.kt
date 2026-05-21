package com.kalos.app.core.domain.model

data class NutritionGoal(
    val kcal: Int = 2000,
    val proteinG: Int = 150,
    val carbsG: Int = 200,
    val fatG: Int = 67,
    val isCustom: Boolean = false,
)

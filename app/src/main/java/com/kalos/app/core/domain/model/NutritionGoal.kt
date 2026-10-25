package com.kalos.app.core.domain.model

data class NutritionGoal(
    val kcal: Int = DEFAULT_KCAL,
    val proteinG: Int = DEFAULT_PROTEIN_G,
    val carbsG: Int = DEFAULT_CARBS_G,
    val fatG: Int = DEFAULT_FAT_G,
    val isCustom: Boolean = false,
) {
    companion object {
        const val DEFAULT_KCAL = 2000
        const val DEFAULT_PROTEIN_G = 150
        const val DEFAULT_CARBS_G = 200
        const val DEFAULT_FAT_G = 67

        /** Daily kcal goal zone: reached from 90 % of the goal, over above 105 %. */
        const val ZONE_LOW = 0.90f
        const val ZONE_HIGH = 1.05f

        const val DEFAULT_WATER_ML = 2000
        const val MIN_WATER_GOAL_ML = 100
    }
}

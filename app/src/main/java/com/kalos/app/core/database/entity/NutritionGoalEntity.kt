package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kalos.app.core.domain.model.NutritionGoal

@Entity(tableName = "nutrition_goal")
data class NutritionGoalEntity(
    @PrimaryKey val id: Int = 1,
    val kcal: Int = NutritionGoal.DEFAULT_KCAL,
    val proteinG: Int = NutritionGoal.DEFAULT_PROTEIN_G,
    val carbsG: Int = NutritionGoal.DEFAULT_CARBS_G,
    val fatG: Int = NutritionGoal.DEFAULT_FAT_G,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

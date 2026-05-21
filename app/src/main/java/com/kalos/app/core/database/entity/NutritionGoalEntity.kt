package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_goal")
data class NutritionGoalEntity(
    @PrimaryKey val id: Int = 1,
    val kcal: Int = 2000,
    val proteinG: Int = 150,
    val carbsG: Int = 200,
    val fatG: Int = 67,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

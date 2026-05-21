package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents one meal slot on a given date (e.g., Breakfast on 2024-01-15)
@Entity(tableName = "meal_entry")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // ISO date: "2024-01-15"
    val mealType: String,      // BREAKFAST | LUNCH | DINNER | SNACK
)

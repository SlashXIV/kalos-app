package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Represents one meal slot on a given date (e.g., Breakfast on 2024-01-15)
// Indexed on `date`: every date-scoped query (day view, history, summaries, MIN(date))
// hits this column, and the table grows one row per meal per day.
@Entity(tableName = "meal_entry", indices = [Index("date")])
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // ISO date: "2024-01-15"
    val mealType: String,      // BREAKFAST | LUNCH | DINNER | SNACK
)

package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Represents one meal slot on a given date (e.g., Breakfast on 2024-01-15).
// Unique (date, mealType): one slot per meal per day. The index leads with `date`, so it also
// serves every date-scoped query (day view, history, summaries, MIN(date)).
@Entity(tableName = "meal_entry", indices = [Index(value = ["date", "mealType"], unique = true)])
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // ISO date: "2024-01-15"
    val mealType: String,      // BREAKFAST | LUNCH | DINNER | SNACK
)

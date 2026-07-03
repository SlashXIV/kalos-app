package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// A reusable "favourite meal": a named set of foods + amounts, applied to a meal in one tap.
@Entity(tableName = "meal_template")
data class MealTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

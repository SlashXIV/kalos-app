package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_program")
data class TrainingProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val durationWeeks: Int = 4,
    val daysPerWeek: Int = 3,
    val isActive: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

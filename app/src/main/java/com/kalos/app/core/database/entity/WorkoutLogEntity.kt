package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_log")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long? = null,
    val templateName: String = "",
    val date: String,           // ISO date: "2024-01-15"
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationSecs: Long = 0,
    val notes: String = "",
    val totalVolumeKg: Float = 0f,
)

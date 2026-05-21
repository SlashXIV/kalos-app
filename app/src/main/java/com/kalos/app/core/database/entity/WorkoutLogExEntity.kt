package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_log_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("logId")],
)
data class WorkoutLogExEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val orderIndex: Int,
)

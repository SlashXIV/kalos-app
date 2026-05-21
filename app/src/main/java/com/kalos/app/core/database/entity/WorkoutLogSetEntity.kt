package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_log_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogExEntity::class,
            parentColumns = ["id"],
            childColumns = ["logExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("logExerciseId")],
)
data class WorkoutLogSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logExerciseId: Long,
    val setNumber: Int,
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSecs: Int = 0,
    val isCompleted: Boolean = false,
    val rpe: Int? = null,       // Rate of Perceived Exertion 1-10
)

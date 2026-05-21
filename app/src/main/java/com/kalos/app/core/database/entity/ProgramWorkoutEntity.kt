package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_workout",
    foreignKeys = [
        ForeignKey(
            entity = TrainingProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId"), Index("templateId")],
)
data class ProgramWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val templateId: Long,
    val dayOfWeek: Int,     // 1=Mon .. 7=Sun
    val weekNumber: Int = 1,
)

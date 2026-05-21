package com.kalos.app.core.domain.model

data class WorkoutTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val estimatedDurationMin: Int = 60,
    val exercises: List<TemplateExercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class TemplateExercise(
    val id: Long = 0,
    val templateId: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultWeightKg: Float = 0f,
    val restSeconds: Int = 90,
    val notes: String = "",
)

package com.kalos.app.core.domain.model

data class TrainingProgram(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val durationWeeks: Int = 4,
    val daysPerWeek: Int = 3,
    val isActive: Boolean = false,
    val workouts: List<ProgramWorkout> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class ProgramWorkout(
    val id: Long = 0,
    val programId: Long,
    val template: WorkoutTemplate?,
    val dayOfWeek: Int,   // 1=Mon .. 7=Sun
    val weekNumber: Int = 1,
)

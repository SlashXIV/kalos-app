package com.kalos.app.core.domain.model

data class WorkoutLog(
    val id: Long = 0,
    val templateId: Long? = null,
    val templateName: String = "",
    val date: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationSecs: Long = 0,
    val notes: String = "",
    val totalVolumeKg: Float = 0f,
    val exercises: List<LogExercise> = emptyList(),
)

data class LogExercise(
    val id: Long = 0,
    val logId: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val sets: List<WorkoutSet> = emptyList(),
) {
    val totalVolume get() = sets.filter { it.isCompleted }.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
}

data class WorkoutSet(
    val id: Long = 0,
    val logExerciseId: Long,
    val setNumber: Int,
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSecs: Int = 0,
    val isCompleted: Boolean = false,
    val rpe: Int? = null,
)

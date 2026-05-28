package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    // Templates
    fun getTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun getTemplate(id: Long): WorkoutTemplate?
    suspend fun saveTemplate(template: WorkoutTemplate): Long
    suspend fun deleteTemplate(template: WorkoutTemplate)

    // Logs
    fun getLogs(): Flow<List<WorkoutLog>>
    fun getLogsForDate(date: String): Flow<List<WorkoutLog>>
    fun getTrainedDates(): Flow<List<String>>
    suspend fun getLog(id: Long): WorkoutLog?
    suspend fun startLog(log: WorkoutLog): Long
    suspend fun finishLog(logId: Long, durationSecs: Long, totalVolume: Float)
    suspend fun upsertSet(logId: Long, exerciseId: Long, set: WorkoutSet): Long
    suspend fun getMaxWeight(exerciseId: Long): Float?
    suspend fun getMaxWeightsForExercises(exerciseIds: List<Long>): Map<Long, Float?>

    /**
     * Persists a complete workout atomically: log + exercises + sets + duration/volume.
     * On any failure, the entire transaction rolls back — no partial logs in history.
     *
     * The caller passes a [WorkoutLog] with id=0, exercises filled out (with their sets,
     * statuses, etc.). The repository computes totalVolume from completed sets.
     *
     * @return the new logId.
     */
    suspend fun completeWorkout(log: WorkoutLog, durationSecs: Long): Long

    /**
     * Updates a single set and recomputes the log's totalVolume atomically.
     * Returns the reloaded log on success, null if the log was not found.
     */
    suspend fun editSet(logId: Long, exerciseId: Long, set: WorkoutSet): WorkoutLog?

    // Exercise progression
    suspend fun getExerciseProgression(exerciseId: Long): List<Pair<String, Float>>

    // Body weight
    suspend fun logBodyWeight(date: String, weightKg: Float)
    fun getBodyWeightHistory(): Flow<List<Pair<String, Float>>>
}

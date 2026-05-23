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

    // Exercise progression
    suspend fun getExerciseProgression(exerciseId: Long): List<Pair<String, Float>>

    // Body weight
    suspend fun logBodyWeight(date: String, weightKg: Float)
    fun getBodyWeightHistory(): Flow<List<Pair<String, Float>>>
}

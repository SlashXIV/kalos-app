package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.BodyWeightEntity
import com.kalos.app.core.database.entity.WorkoutLogEntity
import com.kalos.app.core.database.entity.WorkoutLogExEntity
import com.kalos.app.core.database.entity.WorkoutLogSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_log ORDER BY startedAt DESC")
    fun getAll(): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_log WHERE date = :date")
    fun getForDate(date: String): Flow<List<WorkoutLogEntity>>

    @Query("SELECT DISTINCT date FROM workout_log ORDER BY date DESC LIMIT 60")
    fun getTrainedDates(): Flow<List<String>>

    @Query("SELECT * FROM workout_log WHERE id = :id")
    suspend fun getById(id: Long): WorkoutLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLogEntity): Long

    @Update
    suspend fun updateLog(log: WorkoutLogEntity)

    @Delete
    suspend fun deleteLog(log: WorkoutLogEntity)

    // Log exercises
    @Query("SELECT * FROM workout_log_exercise WHERE logId = :logId ORDER BY orderIndex ASC")
    fun getExercisesForLog(logId: Long): Flow<List<WorkoutLogExEntity>>

    @Query("SELECT * FROM workout_log_exercise WHERE logId = :logId ORDER BY orderIndex ASC")
    suspend fun getExercisesForLogOnce(logId: Long): List<WorkoutLogExEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogExercise(ex: WorkoutLogExEntity): Long

    // Sets
    @Query("SELECT * FROM workout_log_set WHERE logExerciseId = :logExerciseId ORDER BY setNumber ASC")
    fun getSetsForExercise(logExerciseId: Long): Flow<List<WorkoutLogSetEntity>>

    @Query("SELECT * FROM workout_log_set WHERE logExerciseId = :logExerciseId ORDER BY setNumber ASC")
    suspend fun getSetsForExerciseOnce(logExerciseId: Long): List<WorkoutLogSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: WorkoutLogSetEntity): Long

    @Delete
    suspend fun deleteSet(set: WorkoutLogSetEntity)

    // Personal records: max weight per exercise
    @Query("""
        SELECT wls.weightKg
        FROM workout_log_set wls
        JOIN workout_log_exercise wle ON wls.logExerciseId = wle.id
        WHERE wle.exerciseId = :exerciseId AND wls.isCompleted = 1
        ORDER BY wls.weightKg DESC
        LIMIT 1
    """)
    suspend fun getMaxWeight(exerciseId: Long): Float?

    // Exercise progression
    data class ExerciseProgressionRow(val date: String, val maxWeight: Float)

    @Query("""
        SELECT wl.date AS date, MAX(wls.weightKg) AS maxWeight
        FROM workout_log_set wls
        JOIN workout_log_exercise wle ON wls.logExerciseId = wle.id
        JOIN workout_log wl ON wle.logId = wl.id
        WHERE wle.exerciseId = :exerciseId
          AND wls.isCompleted = 1
          AND wls.weightKg > 0
        GROUP BY wl.id
        ORDER BY wl.date ASC
        LIMIT :limit
    """)
    suspend fun getExerciseProgression(exerciseId: Long, limit: Int = 20): List<ExerciseProgressionRow>

    // Body weight
    @Insert
    suspend fun insertBodyWeight(entry: BodyWeightEntity)

    @Query("UPDATE body_weight SET weightKg = :weightKg WHERE date = :date")
    suspend fun updateBodyWeightForDate(date: String, weightKg: Float): Int

    @Query("SELECT * FROM body_weight ORDER BY date DESC LIMIT 60")
    fun getBodyWeightHistory(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight WHERE date = :date LIMIT 1")
    suspend fun getBodyWeightForDate(date: String): BodyWeightEntity?
}

package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.ProgramWorkoutEntity
import com.kalos.app.core.database.entity.TrainingProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM training_program ORDER BY isActive DESC, createdAt DESC")
    fun getAll(): Flow<List<TrainingProgramEntity>>

    @Query("SELECT * FROM training_program WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<TrainingProgramEntity?>

    @Query("SELECT * FROM training_program WHERE id = :id")
    suspend fun getById(id: Long): TrainingProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(program: TrainingProgramEntity): Long

    @Delete
    suspend fun delete(program: TrainingProgramEntity)

    @Query("UPDATE training_program SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE training_program SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    // Program workouts
    @Query("SELECT * FROM program_workout WHERE programId = :programId ORDER BY weekNumber ASC, dayOfWeek ASC")
    fun getWorkoutsForProgram(programId: Long): Flow<List<ProgramWorkoutEntity>>

    @Query("SELECT * FROM program_workout WHERE programId = :programId ORDER BY weekNumber ASC, dayOfWeek ASC")
    suspend fun getWorkoutsForProgramOnce(programId: Long): List<ProgramWorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: ProgramWorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: ProgramWorkoutEntity)

    @Query("DELETE FROM program_workout WHERE programId = :programId")
    suspend fun deleteAllWorkoutsForProgram(programId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(programs: List<TrainingProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllWorkouts(workouts: List<ProgramWorkoutEntity>)

    @Query("SELECT COUNT(*) FROM training_program")
    suspend fun count(): Int
}

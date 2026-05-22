package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.WorkoutTemplateEntity
import com.kalos.app.core.database.entity.WorkoutTemplateExEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_template ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_template WHERE id = :id")
    suspend fun getById(id: Long): WorkoutTemplateEntity?

    @Query("SELECT * FROM workout_template WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: WorkoutTemplateEntity): Long

    @Delete
    suspend fun delete(template: WorkoutTemplateEntity)

    // Template exercises
    @Query("SELECT * FROM workout_template_exercise WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun getExercisesForTemplate(templateId: Long): Flow<List<WorkoutTemplateExEntity>>

    @Query("SELECT * FROM workout_template_exercise WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getExercisesForTemplateOnce(templateId: Long): List<WorkoutTemplateExEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: WorkoutTemplateExEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<WorkoutTemplateExEntity>)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutTemplateExEntity)

    @Query("DELETE FROM workout_template_exercise WHERE templateId = :templateId")
    suspend fun deleteAllExercisesForTemplate(templateId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<WorkoutTemplateExEntity>)
}

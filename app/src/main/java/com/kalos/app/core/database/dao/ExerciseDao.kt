package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE primaryMuscle = :muscle ORDER BY name ASC")
    fun getByMuscle(muscle: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE type = :type ORDER BY name ASC")
    fun getByType(type: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE level = :level ORDER BY name ASC")
    fun getByLevel(level: String): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercise
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
        AND (:muscle = '' OR primaryMuscle = :muscle)
        AND (:type = '' OR type = :type)
        AND (:equipment = '' OR equipment = :equipment)
        AND (:onlyFavorites = 0 OR isFavorite = 1)
        ORDER BY name ASC
    """)
    fun filter(query: String, muscle: String, type: String, equipment: String, onlyFavorites: Boolean = false): Flow<List<ExerciseEntity>>

    @Query("UPDATE exercise SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT primaryMuscle FROM exercise ORDER BY primaryMuscle ASC")
    suspend fun getMuscleGroups(): List<String>

    @Query("SELECT DISTINCT equipment FROM exercise ORDER BY equipment ASC")
    suspend fun getEquipmentTypes(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exercise: ExerciseEntity): Long

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int
}

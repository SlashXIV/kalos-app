package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun filter(query: String, muscle: String, type: String, equipment: String): Flow<List<Exercise>>
    fun getAll(): Flow<List<Exercise>>
    suspend fun getById(id: Long): Exercise?
    suspend fun getMuscleGroups(): List<String>
    suspend fun getEquipmentTypes(): List<String>
    suspend fun save(exercise: Exercise): Long
    suspend fun delete(exercise: Exercise)
    suspend fun count(): Int
}

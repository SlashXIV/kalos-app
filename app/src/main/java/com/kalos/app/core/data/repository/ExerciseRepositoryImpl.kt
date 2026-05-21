package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.data.mapper.toEntity
import com.kalos.app.core.database.dao.ExerciseDao
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(private val dao: ExerciseDao) : ExerciseRepository {
    override fun filter(query: String, muscle: String, type: String, equipment: String): Flow<List<Exercise>> =
        dao.filter(query, muscle, type, equipment).map { list -> list.map { it.toDomain() } }
    override fun getAll(): Flow<List<Exercise>> = dao.getAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getById(id: Long): Exercise? = dao.getById(id)?.toDomain()
    override suspend fun getMuscleGroups(): List<String> = dao.getMuscleGroups()
    override suspend fun getEquipmentTypes(): List<String> = dao.getEquipmentTypes()
    override suspend fun save(exercise: Exercise): Long = dao.upsert(exercise.toEntity())
    override suspend fun delete(exercise: Exercise) = dao.delete(exercise.toEntity())
    override suspend fun count(): Int = dao.count()
}

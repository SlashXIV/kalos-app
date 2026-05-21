package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.TrainingProgram
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun getAll(): Flow<List<TrainingProgram>>
    fun getActive(): Flow<TrainingProgram?>
    suspend fun getById(id: Long): TrainingProgram?
    suspend fun save(program: TrainingProgram): Long
    suspend fun delete(program: TrainingProgram)
    suspend fun activate(id: Long)
}

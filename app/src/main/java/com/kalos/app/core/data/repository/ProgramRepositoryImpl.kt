package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.database.dao.ProgramDao
import com.kalos.app.core.database.dao.WorkoutTemplateDao
import com.kalos.app.core.database.dao.ExerciseDao
import com.kalos.app.core.database.entity.ProgramWorkoutEntity
import com.kalos.app.core.database.entity.TrainingProgramEntity
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgramRepositoryImpl @Inject constructor(
    private val dao: ProgramDao,
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
) : ProgramRepository {

    override fun getAll(): Flow<List<TrainingProgram>> = dao.getAll().map { programs ->
        programs.map { buildProgram(it) }
    }

    override fun getActive(): Flow<TrainingProgram?> = dao.getActive().map { it?.let { buildProgram(it) } }

    override suspend fun getById(id: Long): TrainingProgram? {
        return dao.getById(id)?.let { buildProgram(it) }
    }

    private suspend fun buildProgram(entity: TrainingProgramEntity): TrainingProgram {
        val workoutEntities = dao.getWorkoutsForProgram(entity.id)
        return TrainingProgram(
            id = entity.id, name = entity.name, description = entity.description,
            durationWeeks = entity.durationWeeks, daysPerWeek = entity.daysPerWeek,
            isActive = entity.isActive, createdAt = entity.createdAt,
        )
    }

    override suspend fun save(program: TrainingProgram): Long {
        val id = dao.upsert(TrainingProgramEntity(
            id = program.id, name = program.name, description = program.description,
            durationWeeks = program.durationWeeks, daysPerWeek = program.daysPerWeek,
            isActive = program.isActive))
        dao.deleteAllWorkoutsForProgram(id)
        program.workouts.forEach { pw ->
            pw.template?.let {
                dao.upsertWorkout(ProgramWorkoutEntity(
                    programId = id, templateId = it.id,
                    dayOfWeek = pw.dayOfWeek, weekNumber = pw.weekNumber))
            }
        }
        return id
    }

    override suspend fun delete(program: TrainingProgram) =
        dao.delete(TrainingProgramEntity(id = program.id, name = program.name))

    override suspend fun activate(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }
}

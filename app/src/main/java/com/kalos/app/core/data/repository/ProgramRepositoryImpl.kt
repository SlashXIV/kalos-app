package com.kalos.app.core.data.repository

import androidx.room.withTransaction
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.ProgramDao
import com.kalos.app.core.database.dao.WorkoutTemplateDao
import com.kalos.app.core.database.entity.ProgramWorkoutEntity
import com.kalos.app.core.database.entity.TrainingProgramEntity
import com.kalos.app.core.database.entity.WorkoutTemplateEntity
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProgramRepositoryImpl @Inject constructor(
    private val database: KalosDatabase,
    private val dao: ProgramDao,
    private val templateDao: WorkoutTemplateDao,
) : ProgramRepository {

    /** Séances liées, séances et nombre d'exercices : trois requêtes, réémises à chaque modification. */
    private class Links(
        val workoutsByProgram: Map<Long, List<ProgramWorkoutEntity>>,
        val templates: Map<Long, WorkoutTemplateEntity>,
        val exerciseCounts: Map<Long, Int>,
    )

    private val links: Flow<Links> = combine(dao.getAllWorkouts(), templateDao.getAll(), templateDao.getAllExercises()) { workouts, templates, exercises ->
        Links(
            workoutsByProgram = workouts.groupBy { it.programId },
            templates = templates.associateBy { it.id },
            exerciseCounts = exercises.groupingBy { it.templateId }.eachCount(),
        )
    }

    override fun getAll(): Flow<List<TrainingProgram>> =
        combine(dao.getAll(), links) { programs, l -> programs.map { buildProgram(it, l) } }

    override fun getActive(): Flow<TrainingProgram?> =
        combine(dao.getActive(), links) { program, l -> program?.let { buildProgram(it, l) } }

    override suspend fun getById(id: Long): TrainingProgram? {
        return dao.getById(id)?.let { buildProgram(it, links.first()) }
    }

    private fun buildProgram(entity: TrainingProgramEntity, links: Links): TrainingProgram {
        val workouts = links.workoutsByProgram[entity.id].orEmpty().mapNotNull { pw ->
            val t = links.templates[pw.templateId] ?: return@mapNotNull null
            val count = links.exerciseCounts[pw.templateId] ?: 0
            ProgramWorkout(
                id = pw.id, programId = pw.programId,
                template = WorkoutTemplate(
                    id = t.id, name = t.name, description = t.description,
                    estimatedDurationMin = t.estimatedDurationMin,
                    exerciseCount = count,
                ),
                dayOfWeek = pw.dayOfWeek, weekNumber = pw.weekNumber,
            )
        }
        return TrainingProgram(
            id = entity.id, name = entity.name, description = entity.description,
            durationWeeks = entity.durationWeeks, daysPerWeek = entity.daysPerWeek,
            isActive = entity.isActive, isCustom = entity.isCustom, createdAt = entity.createdAt,
            workouts = workouts,
        )
    }

    override suspend fun save(program: TrainingProgram): Long = database.withTransaction {
        val id = dao.upsert(TrainingProgramEntity(
            id = program.id, name = program.name, description = program.description,
            durationWeeks = program.durationWeeks, daysPerWeek = program.daysPerWeek,
            isActive = program.isActive, isCustom = program.isCustom))
            .let { if (program.id > 0) program.id else it }
        dao.deleteAllWorkoutsForProgram(id)
        program.workouts.forEach { pw ->
            pw.template?.let {
                dao.upsertWorkout(ProgramWorkoutEntity(
                    programId = id, templateId = it.id,
                    dayOfWeek = pw.dayOfWeek, weekNumber = pw.weekNumber))
            }
        }
        id
    }

    override suspend fun delete(program: TrainingProgram) =
        dao.delete(TrainingProgramEntity(id = program.id, name = program.name))

    override suspend fun activate(id: Long) = database.withTransaction {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun linkTemplate(templateId: Long, programId: Long, dayOfWeek: Int) {
        database.withTransaction {
            dao.deleteWorkoutByProgramAndDay(programId, dayOfWeek)
            dao.deleteWorkoutByProgramAndTemplate(programId, templateId)
            dao.upsertWorkout(
                ProgramWorkoutEntity(
                    programId = programId,
                    templateId = templateId,
                    dayOfWeek = dayOfWeek,
                )
            )
        }
    }

    override suspend fun unlinkTemplate(templateId: Long, programId: Long) {
        dao.deleteWorkoutByProgramAndTemplate(programId, templateId)
    }

    override suspend fun getLinksForTemplate(templateId: Long): List<ProgramWorkout> {
        return dao.getWorkoutsForTemplate(templateId).mapNotNull { pw ->
            val t = templateDao.getById(pw.templateId) ?: return@mapNotNull null
            ProgramWorkout(
                id = pw.id,
                programId = pw.programId,
                template = WorkoutTemplate(id = t.id, name = t.name, description = t.description,
                    estimatedDurationMin = t.estimatedDurationMin),
                dayOfWeek = pw.dayOfWeek,
                weekNumber = pw.weekNumber,
            )
        }
    }
}

package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.data.mapper.toEntity
import com.kalos.app.core.database.dao.ExerciseDao
import com.kalos.app.core.database.dao.WorkoutLogDao
import com.kalos.app.core.database.dao.WorkoutTemplateDao
import com.kalos.app.core.database.entity.*
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val templateDao: WorkoutTemplateDao,
    private val logDao: WorkoutLogDao,
    private val exerciseDao: ExerciseDao,
) : WorkoutRepository {

    override fun getTemplates(): Flow<List<WorkoutTemplate>> = templateDao.getAll().map { templates ->
        templates.map { t ->
            val exEntities = templateDao.getExercisesForTemplateOnce(t.id)
            val exercises = exEntities.mapNotNull { te ->
                val ex = exerciseDao.getById(te.exerciseId) ?: return@mapNotNull null
                TemplateExercise(
                    id = te.id, templateId = t.id, exercise = ex.toDomain(),
                    orderIndex = te.orderIndex, defaultSets = te.defaultSets,
                    defaultReps = te.defaultReps, defaultWeightKg = te.defaultWeightKg,
                    restSeconds = te.restSeconds, notes = te.notes,
                )
            }
            WorkoutTemplate(id = t.id, name = t.name, description = t.description,
                estimatedDurationMin = t.estimatedDurationMin, exercises = exercises)
        }
    }

    override suspend fun getTemplate(id: Long): WorkoutTemplate? {
        val t = templateDao.getById(id) ?: return null
        val exEntities = templateDao.getExercisesForTemplateOnce(id)
        val exercises = exEntities.mapNotNull { te ->
            val ex = exerciseDao.getById(te.exerciseId) ?: return@mapNotNull null
            TemplateExercise(id = te.id, templateId = id, exercise = ex.toDomain(),
                orderIndex = te.orderIndex, defaultSets = te.defaultSets,
                defaultReps = te.defaultReps, defaultWeightKg = te.defaultWeightKg,
                restSeconds = te.restSeconds, notes = te.notes)
        }
        return WorkoutTemplate(id = t.id, name = t.name, description = t.description,
            estimatedDurationMin = t.estimatedDurationMin, exercises = exercises)
    }

    override suspend fun saveTemplate(template: WorkoutTemplate): Long {
        val id = templateDao.upsert(WorkoutTemplateEntity(
            id = template.id, name = template.name, description = template.description,
            estimatedDurationMin = template.estimatedDurationMin))
        templateDao.deleteAllExercisesForTemplate(id)
        template.exercises.forEachIndexed { i, te ->
            templateDao.upsertExercise(WorkoutTemplateExEntity(
                templateId = id, exerciseId = te.exercise.id, orderIndex = i,
                defaultSets = te.defaultSets, defaultReps = te.defaultReps,
                defaultWeightKg = te.defaultWeightKg, restSeconds = te.restSeconds, notes = te.notes))
        }
        return id
    }

    override suspend fun deleteTemplate(template: WorkoutTemplate) =
        templateDao.delete(WorkoutTemplateEntity(id = template.id, name = template.name))

    override fun getLogs(): Flow<List<WorkoutLog>> = logDao.getAll().map { logs ->
        logs.map { buildLog(it) }
    }

    override fun getLogsForDate(date: String): Flow<List<WorkoutLog>> = logDao.getForDate(date).map { logs ->
        logs.map { buildLog(it) }
    }

    override fun getTrainedDates(): Flow<List<String>> = logDao.getTrainedDates()

    override suspend fun getLog(id: Long): WorkoutLog? {
        val entity = logDao.getById(id) ?: return null
        return buildLog(entity)
    }

    private suspend fun buildLog(entity: WorkoutLogEntity): WorkoutLog {
        val logExercises = logDao.getExercisesForLogOnce(entity.id)
        val exercises = logExercises.map { le ->
            val ex = exerciseDao.getById(le.exerciseId)
            val sets = logDao.getSetsForExerciseOnce(le.id).map { s ->
                WorkoutSet(id = s.id, logExerciseId = s.logExerciseId, setNumber = s.setNumber,
                    reps = s.reps, weightKg = s.weightKg, durationSecs = s.durationSecs,
                    isCompleted = s.isCompleted, rpe = s.rpe)
            }
            LogExercise(id = le.id, logId = entity.id,
                exercise = ex?.toDomain() ?: Exercise(id = le.exerciseId, name = le.exerciseName, primaryMuscle = ""),
                orderIndex = le.orderIndex, sets = sets)
        }
        return WorkoutLog(id = entity.id, templateId = entity.templateId, templateName = entity.templateName,
            date = entity.date, startedAt = entity.startedAt, finishedAt = entity.finishedAt,
            durationSecs = entity.durationSecs, notes = entity.notes, totalVolumeKg = entity.totalVolumeKg,
            exercises = exercises)
    }

    override suspend fun startLog(log: WorkoutLog): Long {
        val logId = logDao.insertLog(WorkoutLogEntity(
            templateId = log.templateId, templateName = log.templateName,
            date = log.date, startedAt = log.startedAt))
        log.exercises.forEachIndexed { i, le ->
            logDao.insertLogExercise(WorkoutLogExEntity(
                logId = logId, exerciseId = le.exercise.id,
                exerciseName = le.exercise.name, orderIndex = i))
        }
        return logId
    }

    override suspend fun finishLog(logId: Long, durationSecs: Long, totalVolume: Float) {
        val entity = logDao.getById(logId) ?: return
        logDao.updateLog(entity.copy(finishedAt = System.currentTimeMillis(),
            durationSecs = durationSecs, totalVolumeKg = totalVolume))
    }

    override suspend fun upsertSet(logId: Long, exerciseId: Long, set: WorkoutSet): Long {
        return logDao.upsertSet(WorkoutLogSetEntity(
            id = set.id, logExerciseId = set.logExerciseId, setNumber = set.setNumber,
            reps = set.reps, weightKg = set.weightKg, durationSecs = set.durationSecs,
            isCompleted = set.isCompleted, rpe = set.rpe))
    }

    override suspend fun getMaxWeight(exerciseId: Long): Float? = logDao.getMaxWeight(exerciseId)

    override suspend fun logBodyWeight(date: String, weightKg: Float) =
        logDao.insertBodyWeight(BodyWeightEntity(date = date, weightKg = weightKg))

    override fun getBodyWeightHistory(): Flow<List<Pair<String, Float>>> =
        logDao.getBodyWeightHistory().map { list -> list.map { it.date to it.weightKg } }
}

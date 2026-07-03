package com.kalos.app.core.data.repository

import androidx.room.withTransaction
import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.data.mapper.toEntity
import com.kalos.app.core.database.KalosDatabase
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
    private val database: KalosDatabase,
    private val templateDao: WorkoutTemplateDao,
    private val logDao: WorkoutLogDao,
    private val exerciseDao: ExerciseDao,
) : WorkoutRepository {

    override fun getTemplates(): Flow<List<WorkoutTemplate>> = templateDao.getAll().map { templates ->
        templates.map { t ->
            val exEntities = templateDao.getExercisesForTemplateOnce(t.id)
            val exerciseMap = exercisesByIdFor(exEntities.map { it.exerciseId })
            val exercises = exEntities.mapNotNull { te ->
                val ex = exerciseMap[te.exerciseId] ?: return@mapNotNull null
                TemplateExercise(
                    id = te.id, templateId = t.id, exercise = ex,
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
        val exerciseMap = exercisesByIdFor(exEntities.map { it.exerciseId })
        val exercises = exEntities.mapNotNull { te ->
            val ex = exerciseMap[te.exerciseId] ?: return@mapNotNull null
            TemplateExercise(id = te.id, templateId = id, exercise = ex,
                orderIndex = te.orderIndex, defaultSets = te.defaultSets,
                defaultReps = te.defaultReps, defaultWeightKg = te.defaultWeightKg,
                restSeconds = te.restSeconds, notes = te.notes)
        }
        return WorkoutTemplate(id = t.id, name = t.name, description = t.description,
            estimatedDurationMin = t.estimatedDurationMin, exercises = exercises)
    }

    /** Batch-resolves exercises by id into domain models — one query instead of N (avoids N+1). */
    private suspend fun exercisesByIdFor(ids: List<Long>): Map<Long, Exercise> {
        val distinct = ids.distinct()
        if (distinct.isEmpty()) return emptyMap()
        return exerciseDao.getByIds(distinct).associate { it.id to it.toDomain() }
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
        val exerciseMap = exercisesByIdFor(logExercises.map { it.exerciseId })
        val exercises = logExercises.map { le ->
            val ex = exerciseMap[le.exerciseId]
            val sets = logDao.getSetsForExerciseOnce(le.id).map { s ->
                WorkoutSet(id = s.id, logExerciseId = s.logExerciseId, setNumber = s.setNumber,
                    reps = s.reps, weightKg = s.weightKg, durationSecs = s.durationSecs,
                    isCompleted = s.isCompleted, rpe = s.rpe)
            }
            LogExercise(id = le.id, logId = entity.id,
                exercise = ex ?: Exercise(id = le.exerciseId, name = le.exerciseName, primaryMuscle = ""),
                orderIndex = le.orderIndex, sets = sets,
                status = runCatching { ExerciseStatus.valueOf(le.status) }.getOrDefault(ExerciseStatus.PLANNED),
                replacedExerciseName = le.replacedExerciseName)
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
                exerciseName = le.exercise.name, orderIndex = i,
                status = le.status.name,
                replacedExerciseName = le.replacedExerciseName))
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

    override suspend fun getMaxWeightsForExercises(exerciseIds: List<Long>): Map<Long, Float?> {
        if (exerciseIds.isEmpty()) return emptyMap()
        val rows = logDao.getMaxWeights(exerciseIds)
        val byId = rows.associate { it.exerciseId to (it.maxWeight as Float?) }
        // Ensure every requested id is present, even if no completed set exists for it.
        return exerciseIds.associateWith { byId[it] }
    }

    override suspend fun completeWorkout(log: WorkoutLog, durationSecs: Long): Long =
        database.withTransaction {
            val logId = logDao.insertLog(WorkoutLogEntity(
                templateId = log.templateId, templateName = log.templateName,
                date = log.date, startedAt = log.startedAt))

            var totalVolume = 0f
            log.exercises.forEachIndexed { i, le ->
                val logExId = logDao.insertLogExercise(WorkoutLogExEntity(
                    logId = logId, exerciseId = le.exercise.id,
                    exerciseName = le.exercise.name, orderIndex = i,
                    status = le.status.name,
                    replacedExerciseName = le.replacedExerciseName))

                if (le.status == ExerciseStatus.SKIPPED) return@forEachIndexed
                le.sets.forEach { set ->
                    if (set.isCompleted) totalVolume += set.reps * set.weightKg
                    logDao.upsertSet(WorkoutLogSetEntity(
                        id = 0, logExerciseId = logExId, setNumber = set.setNumber,
                        reps = set.reps, weightKg = set.weightKg,
                        durationSecs = set.durationSecs,
                        isCompleted = set.isCompleted, rpe = set.rpe))
                }
            }

            val entity = logDao.getById(logId) ?: error("Log $logId disparu en cours de transaction")
            logDao.updateLog(entity.copy(
                finishedAt = System.currentTimeMillis(),
                durationSecs = durationSecs,
                totalVolumeKg = totalVolume,
            ))
            logId
        }

    override suspend fun editSet(logId: Long, exerciseId: Long, set: WorkoutSet): WorkoutLog? =
        database.withTransaction {
            logDao.upsertSet(WorkoutLogSetEntity(
                id = set.id, logExerciseId = set.logExerciseId, setNumber = set.setNumber,
                reps = set.reps, weightKg = set.weightKg, durationSecs = set.durationSecs,
                isCompleted = set.isCompleted, rpe = set.rpe))

            val reloaded = getLog(logId) ?: return@withTransaction null
            val newVolume = reloaded.exercises.flatMap { it.sets }
                .filter { it.isCompleted }
                .sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
            val entity = logDao.getById(logId) ?: return@withTransaction null
            logDao.updateLog(entity.copy(totalVolumeKg = newVolume))
            getLog(logId)
        }

    override suspend fun getExerciseReference(exerciseId: Long): ExerciseReference? {
        val pr = logDao.getMaxWeight(exerciseId) ?: return null
        if (pr <= 0f) return null
        return ExerciseReference(
            prKg = pr,
            lastSessionTopKg = logDao.getLastSessionTopWeight(exerciseId),
            lastSessionSets = logDao.getLastSessionSets(exerciseId)
                .map { SetSummary(reps = it.reps, weightKg = it.weightKg) },
        )
    }

    override suspend fun getExerciseProgression(exerciseId: Long): List<Pair<String, Float>> =
        logDao.getExerciseProgression(exerciseId).map { it.date to it.maxWeight }

    override suspend fun logBodyWeight(date: String, weightKg: Float) {
        val updated = logDao.updateBodyWeightForDate(date, weightKg)
        if (updated == 0) {
            logDao.insertBodyWeight(BodyWeightEntity(date = date, weightKg = weightKg))
        }
    }

    override fun getBodyWeightHistory(): Flow<List<Pair<String, Float>>> =
        logDao.getBodyWeightHistory().map { list ->
            list.distinctBy { it.date }.map { it.date to it.weightKg }
        }
}

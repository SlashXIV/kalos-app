package com.kalos.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.entity.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: KalosDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KalosDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertExistingTemplate_keepsProgramLinks() = runTest {
        val templateDao = db.workoutTemplateDao()
        val programDao = db.programDao()
        val templateId = templateDao.upsert(WorkoutTemplateEntity(name = "Push"))
        val programId = programDao.upsert(TrainingProgramEntity(name = "PPL"))
        programDao.upsertWorkout(ProgramWorkoutEntity(programId = programId, templateId = templateId, dayOfWeek = 1))

        templateDao.upsert(WorkoutTemplateEntity(id = templateId, name = "Push renommé"))
        programDao.upsert(TrainingProgramEntity(id = programId, name = "PPL renommé"))

        assertEquals("Push renommé", templateDao.getById(templateId)?.name)
        assertEquals(1, programDao.getWorkoutsForProgramOnce(programId).size)
        assertEquals(1, programDao.getWorkoutsForTemplate(templateId).size)
    }

    @Test
    fun exerciseProgression_returnsLatestSessionsOldestFirst() = runTest {
        db.exerciseDao().insertAll(listOf(ExerciseEntity(name = "Squat", primaryMuscle = "Quadriceps")))
        val exerciseId = db.exerciseDao().getAllOnce().single().id
        val logDao = db.workoutLogDao()
        (1..25).forEach { day ->
            val logId = logDao.insertLog(WorkoutLogEntity(date = "2026-01-%02d".format(day), startedAt = day.toLong()))
            val logExId = logDao.insertLogExercise(
                WorkoutLogExEntity(logId = logId, exerciseId = exerciseId, exerciseName = "Squat", orderIndex = 0))
            logDao.upsertSet(WorkoutLogSetEntity(logExerciseId = logExId, setNumber = 1, reps = 5,
                weightKg = 100f + day, isCompleted = true))
        }

        val rows = logDao.getExerciseProgression(exerciseId)

        assertEquals(20, rows.size)
        assertEquals("2026-01-06", rows.first().date)
        assertEquals("2026-01-25", rows.last().date)
        assertEquals(125f, rows.last().maxWeight)
    }

    @Test
    fun batchedLogLoadsReturnEveryExerciseAndSet() = runTest {
        db.exerciseDao().insertAll(listOf(ExerciseEntity(name = "Squat", primaryMuscle = "Quadriceps")))
        val exerciseId = db.exerciseDao().getAllOnce().single().id
        val logDao = db.workoutLogDao()
        val logIds = (1..3).map { day ->
            val logId = logDao.insertLog(WorkoutLogEntity(date = "2026-02-0$day", startedAt = day.toLong()))
            repeat(2) { i ->
                val logExId = logDao.insertLogExercise(
                    WorkoutLogExEntity(logId = logId, exerciseId = exerciseId, exerciseName = "Squat", orderIndex = i))
                repeat(3) { n -> logDao.upsertSet(WorkoutLogSetEntity(logExerciseId = logExId, setNumber = n + 1)) }
            }
            logId
        }

        assertEquals(4, logDao.getExercisesForLogs(logIds.take(2)).size)
        assertEquals(12, logDao.getSetsForLogs(logIds.take(2)).size)
        assertEquals(18, logDao.getSetsForLogs(logIds).size)
    }

    @Test
    fun exportIncludesArchivedCustomFoods() = runTest {
        db.foodDao().insertAll(listOf(
            food("Galette maison", isArchived = false),
            food("Ancienne recette", isArchived = true),
        ))
        assertEquals(
            listOf("Ancienne recette", "Galette maison"),
            db.exportDao().getCustomFoods().map { it.name },
        )
    }

    private fun food(name: String, isArchived: Boolean) = FoodEntity(
        name = name, brand = "", category = "Plats", kcalPer100g = 200f, proteinPer100g = 10f,
        carbsPer100g = 20f, fatPer100g = 8f, fiberPer100g = 2f, defaultServingG = 100f,
        servingUnit = "g", isCustom = true, isFavorite = false, lastUsedAt = 0, isArchived = isArchived,
    )
}

package com.kalos.app.core.export

import android.content.Context
import android.net.Uri
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.database.dao.ExportDao
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExporter @Inject constructor(
    private val exportDao: ExportDao,
    private val dietaryPrefsStore: DietaryPreferencesStore,
    private val waterRepository: WaterRepository,
    @ApplicationContext private val context: Context,
) {
    private val json = Json { prettyPrint = true }

    suspend fun export(uri: Uri): Result<Unit> = runCatching {
        val backup = buildBackup()
        val output = json.encodeToString(backup)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(output.toByteArray(Charsets.UTF_8))
        } ?: error("Impossible d'ouvrir le flux de sortie")
    }

    private suspend fun buildBackup(): KalosBackup {
        val profile         = exportDao.getProfile()
        val goal            = exportDao.getGoal()
        val customFoods     = exportDao.getCustomFoods()
        val allEntries      = exportDao.getAllMealEntries()
        val allItems        = exportDao.getAllMealItems()
        val waterIntake     = exportDao.getAllWaterIntake()
        val bodyWeight      = exportDao.getAllBodyWeight()
        val templates       = exportDao.getAllTemplates()
        val templateExs     = exportDao.getAllTemplateExercises()
        val logs            = exportDao.getAllWorkoutLogs()
        val logExs          = exportDao.getAllWorkoutLogExercises()
        val logSets         = exportDao.getAllWorkoutLogSets()
        val programs        = exportDao.getAllPrograms()
        val programWorkouts = exportDao.getAllProgramWorkouts()

        val itemsByEntry    = allItems.groupBy { it.mealEntryId }
        val exsByTemplate   = templateExs.groupBy { it.templateId }
        val exsByLog        = logExs.groupBy { it.logId }
        val setsByLogEx     = logSets.groupBy { it.logExerciseId }
        val workoutsByProg  = programWorkouts.groupBy { it.programId }

        return KalosBackup(
            appVersion    = "1.7.0",
            exportedAt    = Instant.now().toString(),
            profile       = profile?.let {
                ProfileBackup(
                    name = it.name, age = it.age, sex = it.sex,
                    heightCm = it.heightCm, weightKg = it.weightKg,
                    targetWeightKg = it.targetWeightKg, activityLevel = it.activityLevel,
                    goal = it.goal, createdAt = it.createdAt,
                )
            },
            nutritionGoal = goal?.let {
                NutritionGoalBackup(
                    kcal = it.kcal, proteinG = it.proteinG, carbsG = it.carbsG,
                    fatG = it.fatG, isCustom = it.isCustom, updatedAt = it.updatedAt,
                )
            },
            dietaryFilters = dietaryPrefsStore.filtersFlow.value.map { it.name },
            waterGoalMl    = waterRepository.getGoalMl(),
            customFoods    = customFoods.map {
                FoodBackup(
                    id = it.id, name = it.name, brand = it.brand, category = it.category,
                    kcalPer100g = it.kcalPer100g, proteinPer100g = it.proteinPer100g,
                    carbsPer100g = it.carbsPer100g, fatPer100g = it.fatPer100g,
                    fiberPer100g = it.fiberPer100g, defaultServingG = it.defaultServingG,
                    servingUnit = it.servingUnit, isFavorite = it.isFavorite, tags = it.tags,
                )
            },
            mealEntries = allEntries.map { entry ->
                MealEntryBackup(
                    id = entry.id, date = entry.date, mealType = entry.mealType,
                    items = (itemsByEntry[entry.id] ?: emptyList()).map { item ->
                        MealItemBackup(
                            foodId = item.foodId, amountG = item.amountG, kcal = item.kcal,
                            proteinG = item.proteinG, carbsG = item.carbsG, fatG = item.fatG,
                        )
                    },
                )
            },
            waterIntake = waterIntake.map { WaterIntakeBackup(it.date, it.totalMl) },
            bodyWeight  = bodyWeight.map { BodyWeightBackup(it.date, it.weightKg, it.note) },
            workoutTemplates = templates.map { t ->
                WorkoutTemplateBackup(
                    id = t.id, name = t.name, description = t.description,
                    estimatedDurationMin = t.estimatedDurationMin, createdAt = t.createdAt,
                    exercises = (exsByTemplate[t.id] ?: emptyList()).map { ex ->
                        TemplateExerciseBackup(
                            exerciseId = ex.exerciseId, orderIndex = ex.orderIndex,
                            defaultSets = ex.defaultSets, defaultReps = ex.defaultReps,
                            defaultWeightKg = ex.defaultWeightKg, restSeconds = ex.restSeconds,
                            notes = ex.notes,
                        )
                    },
                )
            },
            workoutLogs = logs.map { log ->
                WorkoutLogBackup(
                    id = log.id, templateId = log.templateId, templateName = log.templateName,
                    date = log.date, startedAt = log.startedAt, finishedAt = log.finishedAt,
                    durationSecs = log.durationSecs, notes = log.notes,
                    totalVolumeKg = log.totalVolumeKg,
                    exercises = (exsByLog[log.id] ?: emptyList()).map { ex ->
                        LogExerciseBackup(
                            exerciseId = ex.exerciseId, exerciseName = ex.exerciseName,
                            orderIndex = ex.orderIndex,
                            sets = (setsByLogEx[ex.id] ?: emptyList()).map { s ->
                                LogSetBackup(
                                    setNumber = s.setNumber, reps = s.reps,
                                    weightKg = s.weightKg, durationSecs = s.durationSecs,
                                    isCompleted = s.isCompleted, rpe = s.rpe,
                                )
                            },
                        )
                    },
                )
            },
            trainingPrograms = programs.map { prog ->
                TrainingProgramBackup(
                    id = prog.id, name = prog.name, description = prog.description,
                    durationWeeks = prog.durationWeeks, daysPerWeek = prog.daysPerWeek,
                    isActive = prog.isActive, createdAt = prog.createdAt,
                    workouts = (workoutsByProg[prog.id] ?: emptyList()).map { w ->
                        ProgramWorkoutBackup(
                            templateId = w.templateId, dayOfWeek = w.dayOfWeek,
                            weekNumber = w.weekNumber,
                        )
                    },
                )
            },
        )
    }
}

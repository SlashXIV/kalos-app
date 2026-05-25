package com.kalos.app.core.export

import android.content.Context
import android.net.Uri
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.data.util.normalizeForSearch
import com.kalos.app.core.database.dao.ImportDao
import com.kalos.app.core.database.entity.*
import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImporter @Inject constructor(
    private val importDao: ImportDao,
    private val dietaryPrefsStore: DietaryPreferencesStore,
    private val waterRepository: WaterRepository,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val SUPPORTED_EXPORT_VERSION = 1
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Reads and validates the backup file without writing anything.
     * Call this first; on success pass the result to [import].
     */
    fun readAndValidate(uri: Uri): Result<KalosBackup> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("Impossible de lire le fichier")

        val backup = json.decodeFromString<KalosBackup>(raw)

        require(backup.exportVersion == SUPPORTED_EXPORT_VERSION) {
            "Version du format non supportée (reçu : v${backup.exportVersion}, supporté : v$SUPPORTED_EXPORT_VERSION)"
        }
        require(backup.exportedAt.isNotBlank()) { "Champ exportedAt manquant ou vide" }

        backup
    }

    /**
     * Imports a validated backup.
     * Clears all user data first, then re-inserts in dependency order.
     * Seed data (foods with isCustom=false, exercises) is never touched.
     *
     * ID remapping: every entity with an auto-generated PK is inserted with id=0
     * so Room assigns a new ID. A map of old→new IDs is propagated to child rows.
     *
     * Known limitation (V1): meal items referencing seed foods assume the seed
     * IDs are identical across installs. This holds as long as the seed data and
     * insertion order have not changed between the exporting and importing builds.
     */
    suspend fun import(backup: KalosBackup): Result<Unit> = runCatching {

        // ── 1. Clear user data in FK-safe order ──────────────────────────────
        importDao.clearMealEntries()        // cascades → meal_entry_item
        importDao.clearWorkoutLogs()        // cascades → workout_log_exercise → workout_log_set
        importDao.clearPrograms()           // cascades → program_workout
        importDao.clearWorkoutTemplates()   // cascades → workout_template_exercise
        importDao.clearCustomFoods()        // safe: no more meal_entry_item references
        importDao.clearWaterIntake()
        importDao.clearBodyWeight()

        // ── 2. Profile & nutrition goal ───────────────────────────────────────
        backup.profile?.let { p ->
            importDao.upsertProfile(
                UserProfileEntity(
                    id = 1,
                    name = p.name, age = p.age, sex = p.sex,
                    heightCm = p.heightCm, weightKg = p.weightKg,
                    targetWeightKg = p.targetWeightKg, activityLevel = p.activityLevel,
                    goal = p.goal, createdAt = p.createdAt,
                    onboardingCompleted = true,
                )
            )
        }
        backup.nutritionGoal?.let { g ->
            importDao.upsertGoal(
                NutritionGoalEntity(
                    id = 1,
                    kcal = g.kcal, proteinG = g.proteinG, carbsG = g.carbsG,
                    fatG = g.fatG, isCustom = g.isCustom, updatedAt = g.updatedAt,
                )
            )
        }

        // ── 3. SharedPreferences ──────────────────────────────────────────────
        val restoredFilters = backup.dietaryFilters
            .mapNotNull { runCatching { DietaryFilter.valueOf(it) }.getOrNull() }
            .toSet()
        DietaryFilter.entries.forEach { filter ->
            dietaryPrefsStore.setFilter(filter, filter in restoredFilters)
        }
        waterRepository.setGoalMl(backup.waterGoalMl)

        // ── 4. Custom foods — build old_id → new_id map ───────────────────────
        val foodIdMap = mutableMapOf<Long, Long>()
        backup.customFoods.forEach { f ->
            val newId = importDao.insertFood(
                FoodEntity(
                    id = 0,
                    name = f.name, nameNormalized = f.name.normalizeForSearch(),
                    brand = f.brand, category = f.category,
                    kcalPer100g = f.kcalPer100g, proteinPer100g = f.proteinPer100g,
                    carbsPer100g = f.carbsPer100g, fatPer100g = f.fatPer100g,
                    fiberPer100g = f.fiberPer100g, defaultServingG = f.defaultServingG,
                    servingUnit = f.servingUnit, isFavorite = f.isFavorite,
                    isCustom = true, tags = f.tags,
                )
            )
            foodIdMap[f.id] = newId
        }

        // ── 5. Meal entries ───────────────────────────────────────────────────
        backup.mealEntries.forEach { entry ->
            val newEntryId = importDao.insertMealEntry(
                MealEntryEntity(id = 0, date = entry.date, mealType = entry.mealType)
            )
            entry.items.forEach { item ->
                // Remap custom food IDs; seed food IDs are assumed stable.
                val resolvedFoodId = foodIdMap[item.foodId] ?: item.foodId
                importDao.insertMealItem(
                    MealEntryItemEntity(
                        id = 0, mealEntryId = newEntryId, foodId = resolvedFoodId,
                        amountG = item.amountG, kcal = item.kcal,
                        proteinG = item.proteinG, carbsG = item.carbsG, fatG = item.fatG,
                    )
                )
            }
        }

        // ── 6. Water intake & body weight ─────────────────────────────────────
        backup.waterIntake.forEach { w ->
            importDao.insertWaterIntake(WaterIntakeEntity(date = w.date, totalMl = w.totalMl))
        }
        backup.bodyWeight.forEach { bw ->
            importDao.insertBodyWeight(
                BodyWeightEntity(id = 0, date = bw.date, weightKg = bw.weightKg, note = bw.note)
            )
        }

        // ── 7. Workout templates — build old_id → new_id map ─────────────────
        val templateIdMap = mutableMapOf<Long, Long>()
        backup.workoutTemplates.forEach { t ->
            val newTemplateId = importDao.insertWorkoutTemplate(
                WorkoutTemplateEntity(
                    id = 0, name = t.name, description = t.description,
                    estimatedDurationMin = t.estimatedDurationMin,
                    createdAt = t.createdAt, updatedAt = t.createdAt,
                )
            )
            templateIdMap[t.id] = newTemplateId
            t.exercises.forEach { ex ->
                importDao.insertTemplateExercise(
                    WorkoutTemplateExEntity(
                        id = 0, templateId = newTemplateId, exerciseId = ex.exerciseId,
                        orderIndex = ex.orderIndex, defaultSets = ex.defaultSets,
                        defaultReps = ex.defaultReps, defaultWeightKg = ex.defaultWeightKg,
                        restSeconds = ex.restSeconds, notes = ex.notes,
                    )
                )
            }
        }

        // ── 8. Workout logs ───────────────────────────────────────────────────
        backup.workoutLogs.forEach { log ->
            val newLogId = importDao.insertWorkoutLog(
                WorkoutLogEntity(
                    id = 0,
                    templateId = log.templateId?.let { templateIdMap[it] ?: it },
                    templateName = log.templateName, date = log.date,
                    startedAt = log.startedAt, finishedAt = log.finishedAt,
                    durationSecs = log.durationSecs, notes = log.notes,
                    totalVolumeKg = log.totalVolumeKg,
                )
            )
            log.exercises.forEach { ex ->
                val newLogExId = importDao.insertLogExercise(
                    WorkoutLogExEntity(
                        id = 0, logId = newLogId, exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName, orderIndex = ex.orderIndex,
                        status = ex.status, replacedExerciseName = ex.replacedExerciseName,
                    )
                )
                ex.sets.forEach { s ->
                    importDao.insertLogSet(
                        WorkoutLogSetEntity(
                            id = 0, logExerciseId = newLogExId, setNumber = s.setNumber,
                            reps = s.reps, weightKg = s.weightKg, durationSecs = s.durationSecs,
                            isCompleted = s.isCompleted, rpe = s.rpe,
                        )
                    )
                }
            }
        }

        // ── 9. Training programs ──────────────────────────────────────────────
        backup.trainingPrograms.forEach { prog ->
            val newProgId = importDao.insertProgram(
                TrainingProgramEntity(
                    id = 0, name = prog.name, description = prog.description,
                    durationWeeks = prog.durationWeeks, daysPerWeek = prog.daysPerWeek,
                    isActive = prog.isActive, createdAt = prog.createdAt,
                )
            )
            prog.workouts.forEach { w ->
                val resolvedTemplateId = templateIdMap[w.templateId] ?: w.templateId
                importDao.insertProgramWorkout(
                    ProgramWorkoutEntity(
                        id = 0, programId = newProgId, templateId = resolvedTemplateId,
                        dayOfWeek = w.dayOfWeek, weekNumber = w.weekNumber,
                    )
                )
            }
        }
    }
}

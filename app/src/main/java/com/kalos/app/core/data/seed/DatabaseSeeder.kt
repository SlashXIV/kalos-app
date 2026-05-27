package com.kalos.app.core.data.seed

import android.content.Context
import com.kalos.app.core.database.dao.ExerciseDao
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.dao.ProgramDao
import com.kalos.app.core.database.entity.ExerciseEntity
import com.kalos.app.core.data.util.normalizeForSearch
import com.kalos.app.core.database.entity.FoodEntity
import com.kalos.app.core.database.entity.TrainingProgramEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
    private val exerciseDao: ExerciseDao,
    private val programDao: ProgramDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().readText().trimStart('﻿')

    // Bump this whenever seed_exercises.json gains new entries or nameNormalized needs backfilling.
    private val SEED_EXERCISES_VERSION = 3

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (foodDao.count() == 0) seedFoods()
        seedExercisesDifferential()
        if (programDao.count() == 0) seedPrograms()
    }

    private suspend fun seedExercisesDifferential() {
        val prefs = context.getSharedPreferences("kalos_seed", Context.MODE_PRIVATE)
        if (prefs.getInt("seed_exercises_version", 0) >= SEED_EXERCISES_VERSION) return

        val raw = readAsset("seed_exercises.json")
        val seeds: List<SeedExercise> = json.decodeFromString(raw)

        if (exerciseDao.count() == 0) {
            // Fresh install: bulk-insert all exercises with seedId + nameNormalized populated.
            val entities = seeds.map { s ->
                ExerciseEntity(
                    name = s.name, primaryMuscle = s.primaryMuscle,
                    secondaryMuscles = json.encodeToString(s.secondaryMuscles),
                    equipment = s.equipment, level = s.level, type = s.type,
                    description = s.description, instructions = s.instructions,
                    seedId = s.id.ifEmpty { null },
                    nameNormalized = s.name.normalizeForSearch(),
                )
            }
            exerciseDao.insertAll(entities)
        } else {
            // Existing install: three-phase differential update.
            //
            // Phase 1 – backfill: assign seedId to existing seed rows (isCustom=0, seedId=NULL)
            // matched by exact name. This prevents Phase 2 from re-inserting exercises already
            // present from a previous build that lacked seedId.
            for (seed in seeds) {
                if (seed.id.isEmpty()) continue
                val existing = exerciseDao.findSeedByNameNoSeedId(seed.name)
                if (existing != null) {
                    exerciseDao.updateSeedId(existing.id, seed.id)
                }
            }

            // Phase 2 – insert: add exercises whose seedId is not yet in the database.
            val existingIds = exerciseDao.getAllSeedIds().toSet()
            for (seed in seeds) {
                if (seed.id.isEmpty() || seed.id in existingIds) continue
                exerciseDao.upsert(
                    ExerciseEntity(
                        name = seed.name, primaryMuscle = seed.primaryMuscle,
                        secondaryMuscles = json.encodeToString(seed.secondaryMuscles),
                        equipment = seed.equipment, level = seed.level, type = seed.type,
                        description = seed.description, instructions = seed.instructions,
                        seedId = seed.id,
                        nameNormalized = seed.name.normalizeForSearch(),
                    )
                )
            }

            // Phase 3 – backfill nameNormalized for all rows that have an empty value
            // (exercises inserted before this column existed).
            val all = exerciseDao.getAllOnce()
            for (ex in all) {
                if (ex.nameNormalized.isEmpty()) {
                    exerciseDao.updateNameNormalized(ex.id, ex.name.normalizeForSearch())
                }
            }
        }

        prefs.edit().putInt("seed_exercises_version", SEED_EXERCISES_VERSION).apply()
    }

    private suspend fun seedFoods() {
        val raw = readAsset("seed_foods.json")
        val seeds: List<SeedFood> = json.decodeFromString(raw)
        val entities = seeds.map { s ->
            FoodEntity(
                name = s.name, nameNormalized = s.name.normalizeForSearch(),
                brand = s.brand, category = s.category,
                kcalPer100g = s.kcal, proteinPer100g = s.protein,
                carbsPer100g = s.carbs, fatPer100g = s.fat,
                fiberPer100g = s.fiber, defaultServingG = s.serving, servingUnit = s.unit,
            )
        }
        foodDao.insertAll(entities)
    }

    private suspend fun seedPrograms() {
        val raw = readAsset("seed_programs.json")
        val seeds: List<SeedProgram> = json.decodeFromString(raw)
        for (seed in seeds) {
            programDao.upsert(
                TrainingProgramEntity(
                    name = seed.name, description = seed.description,
                    durationWeeks = seed.durationWeeks, daysPerWeek = seed.daysPerWeek,
                )
            )
        }
    }
}

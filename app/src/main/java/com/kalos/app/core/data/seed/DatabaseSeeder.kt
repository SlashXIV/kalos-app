package com.kalos.app.core.data.seed

import android.content.Context
import com.kalos.app.core.database.dao.ExerciseDao
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.dao.ProgramDao
import com.kalos.app.core.database.entity.ExerciseEntity
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

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (foodDao.count() == 0) seedFoods()
        if (exerciseDao.count() == 0) seedExercises()
        if (programDao.count() == 0) seedPrograms()
    }

    private suspend fun seedFoods() {
        val raw = context.assets.open("seed_foods.json").bufferedReader().readText()
        val seeds: List<SeedFood> = json.decodeFromString(raw)
        val entities = seeds.map { s ->
            FoodEntity(
                name = s.name, brand = s.brand, category = s.category,
                kcalPer100g = s.kcal, proteinPer100g = s.protein,
                carbsPer100g = s.carbs, fatPer100g = s.fat,
                fiberPer100g = s.fiber, defaultServingG = s.serving, servingUnit = s.unit,
            )
        }
        foodDao.insertAll(entities)
    }

    private suspend fun seedExercises() {
        val raw = context.assets.open("seed_exercises.json").bufferedReader().readText()
        val seeds: List<SeedExercise> = json.decodeFromString(raw)
        val entities = seeds.map { s ->
            ExerciseEntity(
                name = s.name, primaryMuscle = s.primaryMuscle,
                secondaryMuscles = json.encodeToString(s.secondaryMuscles),
                equipment = s.equipment, level = s.level, type = s.type,
                description = s.description, instructions = s.instructions,
            )
        }
        exerciseDao.insertAll(entities)
    }

    private suspend fun seedPrograms() {
        val raw = context.assets.open("seed_programs.json").bufferedReader().readText()
        val seeds: List<SeedProgram> = json.decodeFromString(raw)
        val entities = seeds.map { s ->
            TrainingProgramEntity(
                name = s.name, description = s.description,
                durationWeeks = s.durationWeeks, daysPerWeek = s.daysPerWeek,
            )
        }
        programDao.insertAll(entities)
    }
}

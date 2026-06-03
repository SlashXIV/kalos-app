package com.kalos.app.core.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedFood(
    val name: String,
    val brand: String = "",
    val category: String = "Divers",
    val kcal: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val serving: Float = 100f,
    val unit: String = "g",
)

@Serializable
data class SeedExercise(
    val id: String = "",
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "Aucun",
    val level: String = "Débutant",
    val type: String = "Musculation",
    val trackingMode: String = "REPS_WEIGHT",  // REPS_WEIGHT | DURATION | DURATION_WEIGHT
    val description: String = "",
    val instructions: String = "",
)

@Serializable
data class SeedProgramWorkout(
    val dayOfWeek: Int,
    val weekNumber: Int = 1,
    val templateName: String,
)

@Serializable
data class SeedProgram(
    val name: String,
    val description: String = "",
    val durationWeeks: Int = 4,
    val daysPerWeek: Int = 3,
    val workouts: List<SeedProgramWorkout> = emptyList(),
)

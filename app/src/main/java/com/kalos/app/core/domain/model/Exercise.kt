package com.kalos.app.core.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "Aucun",
    val level: ExerciseLevel = ExerciseLevel.BEGINNER,
    val type: ExerciseType = ExerciseType.STRENGTH,
    val trackingMode: ExerciseTrackingMode = ExerciseTrackingMode.REPS_WEIGHT,
    val description: String = "",
    val instructions: String = "",
    val imageUrl: String = "",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
)

enum class ExerciseLevel(val label: String) {
    BEGINNER("Débutant"),
    INTERMEDIATE("Intermédiaire"),
    ADVANCED("Avancé"),
}

enum class ExerciseType(val label: String) {
    STRENGTH("Musculation"),
    CARDIO("Cardio"),
    BODYWEIGHT("Poids du corps"),
    MOBILITY("Mobilité"),
    HIIT("HIIT"),
}

/**
 * How a set of this exercise is recorded.
 *
 * - REPS_WEIGHT : reps × weight (default — musculation).
 * - DURATION : duration only (cardio, isometric holds like planks).
 * - DURATION_WEIGHT : duration + weight (weighted carries, weighted holds).
 */
enum class ExerciseTrackingMode(val label: String) {
    REPS_WEIGHT("Reps × poids"),
    DURATION("Durée"),
    DURATION_WEIGHT("Durée + poids"),
}

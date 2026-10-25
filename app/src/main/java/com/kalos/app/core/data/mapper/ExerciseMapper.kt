package com.kalos.app.core.data.mapper

import com.kalos.app.core.database.entity.ExerciseEntity
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.model.ExerciseLevel
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.ExerciseType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// Accepts the stable enum name and, for seed files and pre-v18 data, the French label.
fun exerciseLevelOf(raw: String): ExerciseLevel =
    ExerciseLevel.entries.firstOrNull { it.name == raw || it.label == raw } ?: ExerciseLevel.BEGINNER

fun exerciseTypeOf(raw: String): ExerciseType =
    ExerciseType.entries.firstOrNull { it.name == raw || it.label == raw } ?: ExerciseType.STRENGTH

fun ExerciseEntity.toDomain(): Exercise {
    val muscles: List<String> = try {
        json.decodeFromString(secondaryMuscles)
    } catch (e: Exception) { emptyList() }
    return Exercise(
        id = id,
        name = name,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = muscles,
        equipment = equipment,
        level = exerciseLevelOf(level),
        type = exerciseTypeOf(type),
        trackingMode = runCatching { ExerciseTrackingMode.valueOf(trackingMode) }
            .getOrDefault(ExerciseTrackingMode.REPS_WEIGHT),
        description = description,
        instructions = instructions,
        imageUrl = imageUrl,
        isCustom = isCustom,
        isFavorite = isFavorite,
    )
}

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = json.encodeToString(secondaryMuscles),
    equipment = equipment,
    level = level.name,
    type = type.name,
    trackingMode = trackingMode.name,
    description = description,
    instructions = instructions,
    imageUrl = imageUrl,
    isCustom = isCustom,
    isFavorite = isFavorite,
)

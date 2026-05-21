package com.kalos.app.core.data.mapper

import com.kalos.app.core.database.entity.ExerciseEntity
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.model.ExerciseLevel
import com.kalos.app.core.domain.model.ExerciseType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

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
        level = ExerciseLevel.entries.firstOrNull { it.label == level } ?: ExerciseLevel.BEGINNER,
        type = ExerciseType.entries.firstOrNull { it.label == type } ?: ExerciseType.STRENGTH,
        description = description,
        instructions = instructions,
        imageUrl = imageUrl,
        isCustom = isCustom,
    )
}

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = json.encodeToString(secondaryMuscles),
    equipment = equipment,
    level = level.label,
    type = type.label,
    description = description,
    instructions = instructions,
    imageUrl = imageUrl,
    isCustom = isCustom,
)

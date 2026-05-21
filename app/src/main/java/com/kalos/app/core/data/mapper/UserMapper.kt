package com.kalos.app.core.data.mapper

import com.kalos.app.core.database.entity.NutritionGoalEntity
import com.kalos.app.core.database.entity.UserProfileEntity
import com.kalos.app.core.domain.model.*

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    age = age,
    sex = if (sex == "male") Sex.MALE else Sex.FEMALE,
    heightCm = heightCm,
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    activityLevel = ActivityLevel.entries.firstOrNull { it.name.lowercase() == activityLevel } ?: ActivityLevel.MODERATE,
    goal = FitnessGoal.entries.firstOrNull { it.name.lowercase() == goal } ?: FitnessGoal.MAINTAIN,
    onboardingCompleted = onboardingCompleted,
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    name = name,
    age = age,
    sex = if (sex == Sex.MALE) "male" else "female",
    heightCm = heightCm,
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    activityLevel = activityLevel.name.lowercase(),
    goal = goal.name.lowercase(),
    onboardingCompleted = onboardingCompleted,
)

fun NutritionGoalEntity.toDomain() = NutritionGoal(
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    isCustom = isCustom,
)

fun NutritionGoal.toEntity() = NutritionGoalEntity(
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    isCustom = isCustom,
)

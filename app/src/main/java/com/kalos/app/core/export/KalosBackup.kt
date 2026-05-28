package com.kalos.app.core.export

import kotlinx.serialization.Serializable

/**
 * Root backup envelope.
 * exportVersion bumps when the schema changes in a breaking way,
 * so a future importer can branch on it.
 */
@Serializable
data class KalosBackup(
    val exportVersion: Int = 1,
    val appVersion: String,
    val exportedAt: String,           // ISO-8601 instant, e.g. "2026-05-22T10:30:00Z"
    val profile: ProfileBackup?,
    val nutritionGoal: NutritionGoalBackup?,
    val dietaryFilters: List<String>, // DietaryFilter.name values
    val waterGoalMl: Int,
    val customFoods: List<FoodBackup>,
    val mealEntries: List<MealEntryBackup>,
    val waterIntake: List<WaterIntakeBackup>,
    val bodyWeight: List<BodyWeightBackup>,
    val workoutTemplates: List<WorkoutTemplateBackup>,
    val workoutLogs: List<WorkoutLogBackup>,
    val trainingPrograms: List<TrainingProgramBackup>,
)

@Serializable
data class ProfileBackup(
    val name: String,
    val age: Int,
    val sex: String,
    val heightCm: Float,
    val weightKg: Float,
    val targetWeightKg: Float,
    val activityLevel: String,
    val goal: String,
    val createdAt: Long,
    val onboardingCompleted: Boolean = true,  // added v3.7 — default preserves old backups
)

@Serializable
data class NutritionGoalBackup(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val isCustom: Boolean,
    val updatedAt: Long,
)

@Serializable
data class FoodBackup(
    val id: Long,
    val name: String,
    val brand: String,
    val category: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val sugarPer100g: Float = 0f,  // added v3.3 — default preserves old backups
    val defaultServingG: Float,
    val servingUnit: String,
    val isFavorite: Boolean,
    val tags: String,              // comma-separated token string
    val lastUsedAt: Long = 0L,     // added v3.7 — preserves "Recents" ordering after restore
)

@Serializable
data class MealEntryBackup(
    val id: Long,
    val date: String,
    val mealType: String,
    val items: List<MealItemBackup>,
)

@Serializable
data class MealItemBackup(
    val foodId: Long,
    val amountG: Float,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)

@Serializable
data class WaterIntakeBackup(
    val date: String,
    val totalMl: Int,
)

@Serializable
data class BodyWeightBackup(
    val date: String,
    val weightKg: Float,
    val note: String,
)

@Serializable
data class WorkoutTemplateBackup(
    val id: Long,
    val name: String,
    val description: String,
    val estimatedDurationMin: Int,
    val createdAt: Long,
    val exercises: List<TemplateExerciseBackup>,
)

@Serializable
data class TemplateExerciseBackup(
    val exerciseId: Long,
    val orderIndex: Int,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float,
    val restSeconds: Int,
    val notes: String,
)

@Serializable
data class WorkoutLogBackup(
    val id: Long,
    val templateId: Long?,
    val templateName: String,
    val date: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSecs: Long,
    val notes: String,
    val totalVolumeKg: Float,
    val exercises: List<LogExerciseBackup>,
)

@Serializable
data class LogExerciseBackup(
    val exerciseId: Long,
    val exerciseName: String,
    val orderIndex: Int,
    val sets: List<LogSetBackup>,
    val status: String = "PLANNED",
    val replacedExerciseName: String = "",
)

@Serializable
data class LogSetBackup(
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val durationSecs: Int,
    val isCompleted: Boolean,
    val rpe: Int?,
)

@Serializable
data class TrainingProgramBackup(
    val id: Long,
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val isActive: Boolean,
    val isCustom: Boolean = false,  // added v3.3 — default preserves old backups
    val createdAt: Long,
    val workouts: List<ProgramWorkoutBackup>,
)

@Serializable
data class ProgramWorkoutBackup(
    val templateId: Long,
    val dayOfWeek: Int,
    val weekNumber: Int,
)

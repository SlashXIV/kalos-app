package com.kalos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kalos.app.core.database.dao.*
import com.kalos.app.core.database.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        NutritionGoalEntity::class,
        FoodEntity::class,
        MealEntryEntity::class,
        MealEntryItemEntity::class,
        BodyWeightEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExEntity::class,
        WorkoutLogEntity::class,
        WorkoutLogExEntity::class,
        WorkoutLogSetEntity::class,
        TrainingProgramEntity::class,
        ProgramWorkoutEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class KalosDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun programDao(): ProgramDao
}

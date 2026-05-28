package com.kalos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kalos.app.core.database.dao.*
import com.kalos.app.core.database.dao.ExportDao
import com.kalos.app.core.database.dao.ImportDao
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
        WaterIntakeEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
abstract class KalosDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE training_program ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE workout_log_exercise ADD COLUMN status TEXT NOT NULL DEFAULT 'PLANNED'")
                database.execSQL("ALTER TABLE workout_log_exercise ADD COLUMN replacedExerciseName TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise ADD COLUMN nameNormalized TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate exercise table to:
                //  - add nullable seedId column (no explicit DEFAULT → Room expects 'undefined')
                //  - heal the pre-existing isFavorite DEFAULT 0 from MIGRATION_7_8 (same reason)
                //  - avoid a standalone UNIQUE INDEX which Room would reject (not in @Entity indices=[])
                database.execSQL("""
                    CREATE TABLE exercise_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        primaryMuscle TEXT NOT NULL,
                        secondaryMuscles TEXT NOT NULL,
                        equipment TEXT NOT NULL,
                        level TEXT NOT NULL,
                        type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        instructions TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        isCustom INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        seedId TEXT
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO exercise_new
                    SELECT id, name, primaryMuscle, secondaryMuscles, equipment, level, type,
                           description, instructions, imageUrl, isCustom, isFavorite, NULL
                    FROM exercise
                """.trimIndent())
                database.execSQL("DROP TABLE exercise")
                database.execSQL("ALTER TABLE exercise_new RENAME TO exercise")
            }
        }
    }
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun programDao(): ProgramDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun exportDao(): ExportDao
    abstract fun importDao(): ImportDao
}

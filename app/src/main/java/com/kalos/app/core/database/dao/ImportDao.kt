package com.kalos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kalos.app.core.database.entity.*

@Dao
interface ImportDao {

    // ── Clear user data (safe order respects FK RESTRICT / CASCADE) ──────────

    // 1. Meal entries cascade to meal_entry_item (so food RESTRICT is safe after)
    @Query("DELETE FROM meal_entry")
    suspend fun clearMealEntries()

    // 2. Workout logs cascade to workout_log_exercise → workout_log_set
    @Query("DELETE FROM workout_log")
    suspend fun clearWorkoutLogs()

    // 3. Programs cascade to program_workout (references template, which comes next)
    @Query("DELETE FROM training_program")
    suspend fun clearPrograms()

    // 4. Templates cascade to workout_template_exercise
    @Query("DELETE FROM workout_template")
    suspend fun clearWorkoutTemplates()

    // 5. Custom foods — safe now that no meal_entry_item references them
    @Query("DELETE FROM food WHERE isCustom = 1")
    suspend fun clearCustomFoods()

    @Query("DELETE FROM water_intake")
    suspend fun clearWaterIntake()

    @Query("DELETE FROM body_weight")
    suspend fun clearBodyWeight()

    // ── Upsert singleton rows ─────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(entity: NutritionGoalEntity)

    // ── Inserts that return the new auto-generated id ────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(entity: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entity: MealEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItem(entity: MealEntryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(entity: WaterIntakeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(entity: BodyWeightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutTemplate(entity: WorkoutTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(entity: WorkoutTemplateExEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(entity: WorkoutLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogExercise(entity: WorkoutLogExEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogSet(entity: WorkoutLogSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(entity: TrainingProgramEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramWorkout(entity: ProgramWorkoutEntity): Long
}

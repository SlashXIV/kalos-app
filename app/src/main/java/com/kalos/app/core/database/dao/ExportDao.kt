package com.kalos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.kalos.app.core.database.entity.*

@Dao
interface ExportDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM nutrition_goal WHERE id = 1")
    suspend fun getGoal(): NutritionGoalEntity?

    @Query("SELECT * FROM food WHERE isCustom = 1 ORDER BY name ASC")
    suspend fun getCustomFoods(): List<FoodEntity>

    @Query("SELECT * FROM meal_entry ORDER BY date ASC")
    suspend fun getAllMealEntries(): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entry_item")
    suspend fun getAllMealItems(): List<MealEntryItemEntity>

    @Query("SELECT * FROM water_intake ORDER BY date ASC")
    suspend fun getAllWaterIntake(): List<WaterIntakeEntity>

    @Query("SELECT * FROM body_weight ORDER BY date ASC")
    suspend fun getAllBodyWeight(): List<BodyWeightEntity>

    @Query("SELECT * FROM workout_template ORDER BY createdAt ASC")
    suspend fun getAllTemplates(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_template_exercise ORDER BY templateId ASC, orderIndex ASC")
    suspend fun getAllTemplateExercises(): List<WorkoutTemplateExEntity>

    @Query("SELECT * FROM workout_log ORDER BY date ASC")
    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_log_exercise ORDER BY logId ASC, orderIndex ASC")
    suspend fun getAllWorkoutLogExercises(): List<WorkoutLogExEntity>

    @Query("SELECT * FROM workout_log_set ORDER BY logExerciseId ASC, setNumber ASC")
    suspend fun getAllWorkoutLogSets(): List<WorkoutLogSetEntity>

    @Query("SELECT * FROM training_program ORDER BY createdAt ASC")
    suspend fun getAllPrograms(): List<TrainingProgramEntity>

    @Query("SELECT * FROM program_workout ORDER BY programId ASC, weekNumber ASC, dayOfWeek ASC")
    suspend fun getAllProgramWorkouts(): List<ProgramWorkoutEntity>
}

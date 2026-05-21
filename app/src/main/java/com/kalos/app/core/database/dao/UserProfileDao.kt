package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.NutritionGoalEntity
import com.kalos.app.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM nutrition_goal WHERE id = 1")
    fun observeGoal(): Flow<NutritionGoalEntity?>

    @Query("SELECT * FROM nutrition_goal WHERE id = 1")
    suspend fun getGoal(): NutritionGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: NutritionGoalEntity)
}

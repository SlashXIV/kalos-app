package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    fun observeGoal(): Flow<NutritionGoal?>
    suspend fun getGoal(): NutritionGoal?
    suspend fun saveGoal(goal: NutritionGoal)
}

package com.kalos.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun observeTodayIntake(): Flow<Int>
    fun observeWaterForDate(date: String): Flow<Int>
    fun getGoalMl(): Int
    suspend fun addWater(amountMl: Int, date: String)
    fun setGoalMl(goalMl: Int)
}

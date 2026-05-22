package com.kalos.app.core.data.repository

import android.content.Context
import com.kalos.app.core.database.dao.WaterIntakeDao
import com.kalos.app.core.database.entity.WaterIntakeEntity
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val dao: WaterIntakeDao,
    @ApplicationContext context: Context,
) : WaterRepository {

    private val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

    override fun observeTodayIntake(): Flow<Int> =
        dao.observeForDate(LocalDate.now().toString()).map { it ?: 0 }

    override fun observeWaterForDate(date: String): Flow<Int> =
        dao.observeForDate(date).map { it ?: 0 }

    override fun getGoalMl(): Int = prefs.getInt("goal_ml", 2000)

    override suspend fun addWater(amountMl: Int) {
        if (amountMl == 0) return
        val today = LocalDate.now().toString()
        // Ensure the row exists before adjusting (only needed for positive additions)
        if (amountMl > 0) dao.insertIfAbsent(WaterIntakeEntity(date = today, totalMl = 0))
        dao.adjust(today, amountMl)
    }

    override fun setGoalMl(goalMl: Int) {
        prefs.edit().putInt("goal_ml", goalMl).apply()
    }
}

package com.kalos.app.core.domain.repository

import com.kalos.app.core.database.dao.DailySummaryRow
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun getMealsForDate(date: String): Flow<List<MealEntry>>
    suspend fun getOrCreateMealEntry(date: String, mealType: MealType): Long
    suspend fun addItemToMeal(mealEntryId: Long, item: MealItem)
    suspend fun removeItem(itemId: Long)
    fun getDailySummaries(startDate: String, endDate: String): Flow<List<DailySummaryRow>>
    fun getLoggedDates(): Flow<List<String>>
    /** Earliest date with any logged meal, or null if the journal is empty. */
    suspend fun getEarliestMealDate(): String?
}

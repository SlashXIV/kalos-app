package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.database.dao.DailySummaryRow
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.dao.MealEntryDao
import com.kalos.app.core.database.entity.MealEntryEntity
import com.kalos.app.core.database.entity.MealEntryItemEntity
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealEntryDao,
    private val foodDao: FoodDao,
) : MealRepository {

    override fun getMealsForDate(date: String): Flow<List<MealEntry>> {
        return combine(
            mealDao.getEntriesForDate(date),
            mealDao.getAllItemsForDate(date),
        ) { entries, allItems ->
            // Batch all foods in one query instead of N getById calls per emission.
            val itemsByEntry = allItems.groupBy { it.mealEntryId }
            val foodIds = allItems.map { it.foodId }.distinct()
            val foodMap = if (foodIds.isEmpty()) emptyMap()
                          else foodDao.getByIds(foodIds).associateBy { it.id }

            entries.map { entry ->
                val items = itemsByEntry[entry.id] ?: emptyList()
                val mealItems = items.mapNotNull { item ->
                    val food = foodMap[item.foodId] ?: return@mapNotNull null
                    MealItem(
                        id = item.id,
                        mealEntryId = item.mealEntryId,
                        food = food.toDomain(),
                        amountG = item.amountG,
                        kcal = item.kcal,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                    )
                }
                MealEntry(
                    id = entry.id,
                    date = entry.date,
                    mealType = MealType.valueOf(entry.mealType),
                    items = mealItems,
                )
            }
        }
    }

    override suspend fun getOrCreateMealEntry(date: String, mealType: MealType): Long {
        val existing = mealDao.getEntry(date, mealType.name)
        return existing?.id ?: mealDao.insertEntry(
            MealEntryEntity(date = date, mealType = mealType.name)
        )
    }

    override suspend fun addItemToMeal(mealEntryId: Long, item: MealItem) {
        mealDao.insertItem(
            MealEntryItemEntity(
                mealEntryId = mealEntryId,
                foodId = item.food.id,
                amountG = item.amountG,
                kcal = item.kcal,
                proteinG = item.proteinG,
                carbsG = item.carbsG,
                fatG = item.fatG,
            )
        )
        foodDao.updateLastUsed(item.food.id, System.currentTimeMillis())
    }

    override suspend fun removeItem(itemId: Long) = mealDao.deleteItemById(itemId)

    override fun getDailySummaries(startDate: String, endDate: String): Flow<List<DailySummaryRow>> =
        mealDao.getDailySummaries(startDate, endDate)

    override fun getLoggedDates(): Flow<List<String>> = mealDao.getLoggedDates()
}

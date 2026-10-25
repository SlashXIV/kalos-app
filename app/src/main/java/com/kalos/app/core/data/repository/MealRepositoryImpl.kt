package com.kalos.app.core.data.repository

import androidx.room.withTransaction
import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.dao.MealEntryDao
import com.kalos.app.core.database.entity.MealEntryEntity
import com.kalos.app.core.database.entity.MealEntryItemEntity
import com.kalos.app.core.domain.model.DailyTotals
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MealRepositoryImpl @Inject constructor(
    private val database: KalosDatabase,
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

            entries.mapNotNull { entry ->
                // An unknown value (e.g. from a hand-edited backup) must not crash the screen.
                val mealType = MealType.entries.find { it.name == entry.mealType } ?: return@mapNotNull null
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
                    mealType = mealType,
                    items = mealItems,
                )
            }
        }
    }

    override suspend fun getOrCreateMealEntry(date: String, mealType: MealType): Long =
        database.withTransaction {
            mealDao.getEntry(date, mealType.name)?.id
                ?: mealDao.insertEntry(MealEntryEntity(date = date, mealType = mealType.name))
        }

    override suspend fun addItemToMeal(mealEntryId: Long, item: MealItem) = database.withTransaction {
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

    override suspend fun removeItems(itemIds: List<Long>) = database.withTransaction {
        itemIds.forEach { mealDao.deleteItemById(it) }
    }

    override fun getDailySummaries(startDate: String, endDate: String): Flow<List<DailyTotals>> =
        mealDao.getDailySummaries(startDate, endDate).map { rows ->
            rows.map {
                DailyTotals(it.date, it.totalKcal ?: 0f, it.totalProtein ?: 0f, it.totalCarbs ?: 0f, it.totalFat ?: 0f)
            }
        }

    override fun getLoggedDates(): Flow<List<String>> = mealDao.getLoggedDates()

    override suspend fun getEarliestMealDate(): String? = mealDao.getEarliestDate()

    override suspend fun getLatestMealDate(): String? = mealDao.getLatestDate()
}

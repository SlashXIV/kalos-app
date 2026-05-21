package com.kalos.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.MealEntryDao
import com.kalos.app.core.database.entity.FoodEntity
import com.kalos.app.core.database.entity.MealEntryEntity
import com.kalos.app.core.database.entity.MealEntryItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealEntryDaoTest {

    private lateinit var db: KalosDatabase
    private lateinit var mealDao: MealEntryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KalosDatabase::class.java,
        ).allowMainThreadQueries().build()
        mealDao = db.mealEntryDao()

        // Seed a food to satisfy FK constraint
        runTest {
            db.foodDao().upsert(
                FoodEntity(
                    id = 1, name = "Riz", brand = "", category = "Céréales",
                    kcalPer100g = 130f, proteinPer100g = 2.7f, carbsPer100g = 28f,
                    fatPer100g = 0.3f, fiberPer100g = 0.4f, defaultServingG = 100f,
                    servingUnit = "g", isCustom = false, isFavorite = false, lastUsedAt = 0,
                )
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertEntry_andRetrieveByDate() = runTest {
        val entry = MealEntryEntity(date = "2026-01-15", mealType = "BREAKFAST")
        mealDao.insertEntry(entry)
        val results = mealDao.getEntriesForDate("2026-01-15").first()
        assertEquals(1, results.size)
        assertEquals("BREAKFAST", results[0].mealType)
    }

    @Test
    fun getEntry_returnsNullIfNotExisting() = runTest {
        val result = mealDao.getEntry("2026-01-01", "LUNCH")
        assertNull(result)
    }

    @Test
    fun insertItem_andRetrieveForEntry() = runTest {
        val entryId = mealDao.insertEntry(MealEntryEntity(date = "2026-01-15", mealType = "LUNCH"))
        val item = MealEntryItemEntity(
            mealEntryId = entryId, foodId = 1L, amountG = 200f,
            kcal = 260f, proteinG = 5.4f, carbsG = 56f, fatG = 0.6f,
        )
        mealDao.insertItem(item)
        val items = mealDao.getItemsForEntry(entryId).first()
        assertEquals(1, items.size)
        assertEquals(260f, items[0].kcal, 0.01f)
    }

    @Test
    fun deleteItemById_removesItem() = runTest {
        val entryId = mealDao.insertEntry(MealEntryEntity(date = "2026-01-15", mealType = "DINNER"))
        val itemId = mealDao.insertItem(
            MealEntryItemEntity(
                mealEntryId = entryId, foodId = 1L, amountG = 100f,
                kcal = 130f, proteinG = 2.7f, carbsG = 28f, fatG = 0.3f,
            )
        )
        mealDao.deleteItemById(itemId)
        val items = mealDao.getItemsForEntry(entryId).first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun getDailySummaries_aggregatesCorrectly() = runTest {
        val entryId = mealDao.insertEntry(MealEntryEntity(date = "2026-01-15", mealType = "BREAKFAST"))
        mealDao.insertItem(MealEntryItemEntity(
            mealEntryId = entryId, foodId = 1L, amountG = 100f,
            kcal = 130f, proteinG = 2.7f, carbsG = 28f, fatG = 0.3f,
        ))
        mealDao.insertItem(MealEntryItemEntity(
            mealEntryId = entryId, foodId = 1L, amountG = 50f,
            kcal = 65f, proteinG = 1.35f, carbsG = 14f, fatG = 0.15f,
        ))
        val summaries = mealDao.getDailySummaries("2026-01-01", "2026-01-31").first()
        assertEquals(1, summaries.size)
        assertEquals(195f, summaries[0].totalKcal ?: 0f, 0.1f)
    }

    @Test
    fun getLoggedDates_returnsDistinctDates() = runTest {
        mealDao.insertEntry(MealEntryEntity(date = "2026-01-10", mealType = "BREAKFAST"))
        mealDao.insertEntry(MealEntryEntity(date = "2026-01-10", mealType = "LUNCH"))
        mealDao.insertEntry(MealEntryEntity(date = "2026-01-11", mealType = "BREAKFAST"))
        val dates = mealDao.getLoggedDates().first()
        assertEquals(2, dates.size)
    }
}

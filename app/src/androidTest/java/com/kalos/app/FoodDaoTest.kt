package com.kalos.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.entity.FoodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDaoTest {

    private lateinit var db: KalosDatabase
    private lateinit var foodDao: FoodDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KalosDatabase::class.java,
        ).allowMainThreadQueries().build()
        foodDao = db.foodDao()
    }

    @After
    fun tearDown() = db.close()

    private fun food(
        id: Long = 0,
        name: String = "Banane",
        category: String = "Fruits",
        kcal: Float = 89f,
    ) = FoodEntity(
        id = id, name = name, brand = "", category = category,
        kcalPer100g = kcal, proteinPer100g = 1.1f, carbsPer100g = 23f,
        fatPer100g = 0.3f, fiberPer100g = 2.6f, defaultServingG = 100f,
        servingUnit = "g", isCustom = false, isFavorite = false, lastUsedAt = 0,
    )

    @Test
    fun insertAndSearch_returnsMatchingFoods() = runTest {
        foodDao.insertAll(listOf(food(name = "Banane"), food(name = "Pomme"), food(name = "Orange")))
        val results = foodDao.search("ban").first()
        assertEquals(1, results.size)
        assertEquals("Banane", results[0].name)
    }

    @Test
    fun insertAll_ignoresDuplicateId() = runTest {
        val f = food(id = 1, name = "Original")
        foodDao.insertAll(listOf(f))
        foodDao.insertAll(listOf(f.copy(name = "Duplicate")))
        assertEquals(1, foodDao.count())
    }

    @Test
    fun upsert_replacesExistingRecord() = runTest {
        val id = foodDao.upsert(food(id = 1, name = "Original"))
        foodDao.upsert(food(id = id, name = "Updated"))
        val updated = foodDao.getById(id)
        assertEquals("Updated", updated?.name)
    }

    @Test
    fun setFavorite_marksAsFavorite() = runTest {
        val id = foodDao.upsert(food())
        foodDao.setFavorite(id, true)
        val favorites = foodDao.getFavorites().first()
        assertTrue(favorites.any { it.id == id })
    }

    @Test
    fun getRecent_returnsItemsWithLastUsedAt() = runTest {
        val id1 = foodDao.upsert(food(name = "Recent"))
        val id2 = foodDao.upsert(food(name = "Never used"))
        foodDao.updateLastUsed(id1, System.currentTimeMillis())
        val recent = foodDao.getRecent().first()
        assertTrue(recent.any { it.id == id1 })
        assertFalse(recent.any { it.id == id2 })
    }

    @Test
    fun count_returnsCorrectNumber() = runTest {
        assertEquals(0, foodDao.count())
        foodDao.insertAll(listOf(food(name = "A"), food(name = "B"), food(name = "C")))
        assertEquals(3, foodDao.count())
    }

    @Test
    fun delete_removesFood() = runTest {
        val entity = food(name = "ToDelete")
        val id = foodDao.upsert(entity)
        foodDao.delete(entity.copy(id = id))
        assertNull(foodDao.getById(id))
    }
}

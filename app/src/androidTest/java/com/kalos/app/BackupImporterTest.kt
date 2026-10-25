package com.kalos.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.data.repository.WaterRepositoryImpl
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.entity.FoodEntity
import com.kalos.app.core.export.BackupImporter
import com.kalos.app.core.export.KalosBackup
import com.kalos.app.core.export.MealEntryBackup
import com.kalos.app.core.export.MealItemBackup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupImporterTest {

    private lateinit var db: KalosDatabase
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KalosDatabase::class.java).allowMainThreadQueries().build()
        importer = BackupImporter(
            db, db.importDao(), db.foodDao(), db.exerciseDao(),
            DietaryPreferencesStore(context), WaterRepositoryImpl(db, db.waterIntakeDao(), context), context,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun duplicateMealSlotsFromOldBackupsAreMerged() = runTest {
        db.foodDao().insertAll(listOf(FoodEntity(
            id = 1, name = "Riz", brand = "", category = "Féculents", kcalPer100g = 130f, proteinPer100g = 3f,
            carbsPer100g = 28f, fatPer100g = 0f, fiberPer100g = 0f, defaultServingG = 100f, servingUnit = "g",
            isCustom = false, isFavorite = false, lastUsedAt = 0,
        )))
        val item = MealItemBackup(foodId = 1, amountG = 100f, kcal = 130f, proteinG = 3f, carbsG = 28f, fatG = 0f)
        val backup = KalosBackup(
            appVersion = "4.0.2", exportedAt = "2026-10-25T10:00:00Z", profile = null, nutritionGoal = null,
            dietaryFilters = emptyList(), waterGoalMl = 2000, customFoods = emptyList(),
            mealEntries = listOf(
                MealEntryBackup(id = 1, date = "2026-10-25", mealType = "LUNCH", items = listOf(item)),
                MealEntryBackup(id = 2, date = "2026-10-25", mealType = "LUNCH", items = listOf(item, item)),
            ),
            waterIntake = emptyList(), bodyWeight = emptyList(), workoutTemplates = emptyList(),
            workoutLogs = emptyList(), trainingPrograms = emptyList(),
        )

        assertTrue(importer.import(backup).isSuccess)

        assertEquals(1, db.mealEntryDao().getEntriesForDate("2026-10-25").first().size)
        assertEquals(3, db.mealEntryDao().getAllItemsForDate("2026-10-25").first().size)
    }
}

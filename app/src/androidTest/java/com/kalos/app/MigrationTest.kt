package com.kalos.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kalos.app.core.database.KalosDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migrations from the oldest committed schema (13). Versions 7 to 12 have no exported
 * schema and cannot be tested this way.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KalosDatabase::class.java,
    )

    @Test
    fun eachStepMatchesItsExportedSchema() {
        helper.createDatabase(dbName, 13).close()
        helper.runMigrationsAndValidate(dbName, 14, true, KalosDatabase.MIGRATION_13_14).close()
        helper.runMigrationsAndValidate(dbName, 15, true, KalosDatabase.MIGRATION_14_15).close()
        helper.runMigrationsAndValidate(dbName, 16, true, KalosDatabase.MIGRATION_15_16).close()
        helper.runMigrationsAndValidate(dbName, 17, true, KalosDatabase.MIGRATION_16_17).close()
    }

    @Test
    fun userDataSurvivesMigrationTo17() = runTest {
        helper.createDatabase(dbName, 13).apply {
            execSQL(
                "INSERT INTO food (id, name, nameNormalized, brand, category, kcalPer100g, proteinPer100g, " +
                    "carbsPer100g, fatPer100g, fiberPer100g, sugarPer100g, defaultServingG, servingUnit, " +
                    "isCustom, isFavorite, lastUsedAt, tags, isArchived) " +
                    "VALUES (1, 'Galette', 'galette', '', 'Plats', 200, 10, 20, 8, 2, 1, 100, 'g', 1, 0, 0, '', 0)"
            )
            execSQL("INSERT INTO meal_entry (id, date, mealType) VALUES (1, '2026-10-25', 'LUNCH')")
            execSQL(
                "INSERT INTO meal_entry_item (mealEntryId, foodId, amountG, kcal, proteinG, carbsG, fatG) " +
                    "VALUES (1, 1, 150, 300, 15, 30, 12)"
            )
            execSQL(
                "INSERT INTO exercise (id, name, primaryMuscle, secondaryMuscles, equipment, level, type, " +
                    "description, instructions, imageUrl, isCustom, isFavorite, seedId, nameNormalized) " +
                    "VALUES (1, 'Squat', 'Quadriceps', '[]', 'Barre', 'Débutant', 'Musculation', '', '', '', 0, 0, 'squat', 'squat')"
            )
            close()
        }

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(), KalosDatabase::class.java, dbName,
        ).addMigrations(
            KalosDatabase.MIGRATION_13_14, KalosDatabase.MIGRATION_14_15,
            KalosDatabase.MIGRATION_15_16, KalosDatabase.MIGRATION_16_17,
        ).allowMainThreadQueries().build()
        try {
            val meals = db.mealEntryDao().getEntriesForDate("2026-10-25").first()
            assertEquals(1, meals.size)
            assertEquals(300f, db.mealEntryDao().getAllItemsForDate("2026-10-25").first().single().kcal)
            assertEquals(null, db.foodDao().getById(1)?.barcode)
            assertEquals("REPS_WEIGHT", db.exerciseDao().getAllOnce().single().trackingMode)
        } finally {
            db.close()
        }
    }
}

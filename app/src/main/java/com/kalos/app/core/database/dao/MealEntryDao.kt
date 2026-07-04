package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.MealEntryEntity
import com.kalos.app.core.database.entity.MealEntryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    // Meal entries
    @Query("SELECT * FROM meal_entry WHERE date = :date ORDER BY CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END")
    fun getEntriesForDate(date: String): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entry WHERE date = :date AND mealType = :mealType LIMIT 1")
    suspend fun getEntry(date: String, mealType: String): MealEntryEntity?

    // ABORT: caller always passes id=0. REPLACE would CASCADE-delete every item of the
    // conflicting entry — a footgun if anyone ever passes an explicit id.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: MealEntryEntity): Long

    @Delete
    suspend fun deleteEntry(entry: MealEntryEntity)

    // Meal items
    @Query("SELECT * FROM meal_entry_item WHERE mealEntryId = :mealEntryId")
    fun getItemsForEntry(mealEntryId: Long): Flow<List<MealEntryItemEntity>>

    @Query("SELECT mei.* FROM meal_entry_item mei JOIN meal_entry me ON mei.mealEntryId = me.id WHERE me.date = :date")
    fun getAllItemsForDate(date: String): Flow<List<MealEntryItemEntity>>

    // ABORT: caller always passes id=0. No children, but the asymmetry with sibling DAOs is
    // confusing — keep ABORT for consistency.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: MealEntryItemEntity): Long

    @Delete
    suspend fun deleteItem(item: MealEntryItemEntity)

    @Query("DELETE FROM meal_entry_item WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    // Daily summaries
    @Query("""
        SELECT me.date,
               SUM(mei.kcal) as totalKcal,
               SUM(mei.proteinG) as totalProtein,
               SUM(mei.carbsG) as totalCarbs,
               SUM(mei.fatG) as totalFat
        FROM meal_entry me
        LEFT JOIN meal_entry_item mei ON me.id = mei.mealEntryId
        WHERE me.date BETWEEN :startDate AND :endDate
        GROUP BY me.date
    """)
    fun getDailySummaries(startDate: String, endDate: String): Flow<List<DailySummaryRow>>

    // Distinct dates that have any entries
    @Query("SELECT DISTINCT date FROM meal_entry ORDER BY date DESC LIMIT 60")
    fun getLoggedDates(): Flow<List<String>>

    // Earliest logged date — lets the history know whether older data exists beyond the window.
    @Query("SELECT MIN(date) FROM meal_entry")
    suspend fun getEarliestDate(): String?
}

data class DailySummaryRow(
    val date: String,
    val totalKcal: Float?,
    val totalProtein: Float?,
    val totalCarbs: Float?,
    val totalFat: Float?,
)

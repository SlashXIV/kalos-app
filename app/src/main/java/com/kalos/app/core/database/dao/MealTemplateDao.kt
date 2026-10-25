package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.MealTemplateEntity
import com.kalos.app.core.database.entity.MealTemplateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {
    @Query("SELECT * FROM meal_template ORDER BY name ASC")
    fun getAll(): Flow<List<MealTemplateEntity>>

    @Query("SELECT * FROM meal_template WHERE id = :id")
    suspend fun getById(id: Long): MealTemplateEntity?

    @Query("SELECT * FROM meal_template_item WHERE templateId = :templateId")
    suspend fun getItems(templateId: Long): List<MealTemplateItemEntity>

    // @Upsert, not REPLACE: REPLACE deletes the row first, which cascades to its children
    // (program links, items). Returns -1 when an existing row was updated.
    @Upsert
    suspend fun upsertTemplate(template: MealTemplateEntity): Long

    @Query("DELETE FROM meal_template_item WHERE templateId = :templateId")
    suspend fun deleteItemsForTemplate(templateId: Long)

    // ABORT: items are always inserted with id=0; REPLACE would be a footgun with no upside.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: MealTemplateItemEntity): Long

    @Delete
    suspend fun deleteTemplate(template: MealTemplateEntity)

    // How many templates reference this food — used alongside meal_entry_item usage to decide
    // whether a custom food can be hard-deleted or must be archived.
    @Query("SELECT COUNT(*) FROM meal_template_item WHERE foodId = :foodId")
    suspend fun countFoodUsage(foodId: Long): Int

    @Query("SELECT COUNT(*) FROM meal_template")
    suspend fun count(): Int
}

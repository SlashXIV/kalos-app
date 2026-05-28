package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("""
        SELECT * FROM food
        WHERE (nameNormalized LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%')
        AND (:category = '' OR category = :category)
        AND (:onlyCustom = 0 OR isCustom = 1)
        AND isArchived = 0
        ORDER BY isFavorite DESC, lastUsedAt DESC LIMIT 100
    """)
    fun search(query: String, category: String, onlyCustom: Int): Flow<List<FoodEntity>>

    @Query("SELECT DISTINCT category FROM food WHERE isArchived = 0 ORDER BY category ASC")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM food WHERE category = :category AND isArchived = 0 ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE isFavorite = 1 AND isArchived = 0 ORDER BY name ASC")
    fun getFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE lastUsedAt > 0 AND isArchived = 0 ORDER BY lastUsedAt DESC LIMIT 20")
    fun getRecent(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE isCustom = 1 AND isArchived = 0 ORDER BY name ASC")
    fun getCustomFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM food WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FoodEntity>

    @Query("SELECT * FROM food ORDER BY name ASC")
    fun getAll(): Flow<List<FoodEntity>>

    @Query("SELECT COUNT(*) FROM meal_entry_item WHERE foodId = :foodId")
    suspend fun countUsage(foodId: Long): Int

    @Query("SELECT * FROM food WHERE nameNormalized = :nameNormalized AND isArchived = 0 LIMIT 1")
    suspend fun findByNormalizedName(nameNormalized: String): FoodEntity?

    @Query("UPDATE food SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("UPDATE food SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE food SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int
}

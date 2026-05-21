package com.kalos.app.core.database.dao

import androidx.room.*
import com.kalos.app.core.database.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY isFavorite DESC, lastUsedAt DESC LIMIT 100")
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 20")
    fun getRecent(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM food ORDER BY name ASC")
    fun getAll(): Flow<List<FoodEntity>>

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

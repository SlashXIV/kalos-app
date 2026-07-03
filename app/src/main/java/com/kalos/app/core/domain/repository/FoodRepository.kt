package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.Food
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    fun search(query: String, category: String = "", onlyCustom: Boolean = false): Flow<List<Food>>
    fun getDistinctCategories(): Flow<List<String>>
    fun getFavorites(): Flow<List<Food>>
    fun getRecent(): Flow<List<Food>>
    fun getAll(): Flow<List<Food>>
    fun getCustomFoods(): Flow<List<Food>>
    suspend fun getById(id: Long): Food?
    suspend fun save(food: Food): Long
    suspend fun delete(food: Food)
    suspend fun archiveOrDelete(id: Long)
    suspend fun findDuplicate(name: String): Food?
    /** Local barcode-cache lookup (checked before any network resolution). Null if unknown. */
    suspend fun findByBarcode(barcode: String): Food?
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun markUsed(id: Long)
    suspend fun count(): Int
}

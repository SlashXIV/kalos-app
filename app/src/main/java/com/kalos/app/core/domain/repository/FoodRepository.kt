package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.Food
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    fun search(query: String): Flow<List<Food>>
    fun getFavorites(): Flow<List<Food>>
    fun getRecent(): Flow<List<Food>>
    fun getAll(): Flow<List<Food>>
    suspend fun getById(id: Long): Food?
    suspend fun save(food: Food): Long
    suspend fun delete(food: Food)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun markUsed(id: Long)
    suspend fun count(): Int
}

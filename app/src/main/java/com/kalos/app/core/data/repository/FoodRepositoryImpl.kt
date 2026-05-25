package com.kalos.app.core.data.repository

import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.data.mapper.toEntity
import com.kalos.app.core.data.util.normalizeForSearch
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FoodRepositoryImpl @Inject constructor(private val dao: FoodDao) : FoodRepository {
    override fun search(query: String, category: String, onlyCustom: Boolean): Flow<List<Food>> =
        dao.search(query.normalizeForSearch(), category, if (onlyCustom) 1 else 0).map { list -> list.map { it.toDomain() } }
    override fun getDistinctCategories(): Flow<List<String>> = dao.getDistinctCategories()
    override fun getFavorites(): Flow<List<Food>> = dao.getFavorites().map { list -> list.map { it.toDomain() } }
    override fun getRecent(): Flow<List<Food>> = dao.getRecent().map { list -> list.map { it.toDomain() } }
    override fun getAll(): Flow<List<Food>> = dao.getAll().map { list -> list.map { it.toDomain() } }
    override fun getCustomFoods(): Flow<List<Food>> = dao.getCustomFoods().map { list -> list.map { it.toDomain() } }
    override suspend fun getById(id: Long): Food? = dao.getById(id)?.toDomain()
    override suspend fun save(food: Food): Long = dao.upsert(food.toEntity())
    override suspend fun delete(food: Food) = dao.delete(food.toEntity())
    override suspend fun archiveOrDelete(id: Long) {
        if (dao.countUsage(id) == 0) {
            dao.getById(id)?.let { dao.delete(it) }
        } else {
            dao.setArchived(id, true)
        }
    }
    override suspend fun findDuplicate(name: String): Food? =
        dao.findByNormalizedName(name.trim().normalizeForSearch())?.toDomain()
    override suspend fun setFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)
    override suspend fun markUsed(id: Long) = dao.updateLastUsed(id, System.currentTimeMillis())
    override suspend fun count(): Int = dao.count()
}

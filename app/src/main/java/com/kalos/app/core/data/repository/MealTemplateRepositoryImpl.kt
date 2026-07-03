package com.kalos.app.core.data.repository

import androidx.room.withTransaction
import com.kalos.app.core.data.mapper.toDomain
import com.kalos.app.core.database.KalosDatabase
import com.kalos.app.core.database.dao.FoodDao
import com.kalos.app.core.database.dao.MealTemplateDao
import com.kalos.app.core.database.entity.MealTemplateEntity
import com.kalos.app.core.database.entity.MealTemplateItemEntity
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealTemplate
import com.kalos.app.core.domain.model.MealTemplateItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MealTemplateRepositoryImpl @Inject constructor(
    private val database: KalosDatabase,
    private val templateDao: MealTemplateDao,
    private val foodDao: FoodDao,
    private val mealRepository: MealRepository,
) : MealTemplateRepository {

    override fun getTemplates(): Flow<List<MealTemplate>> =
        templateDao.getAll().map { templates ->
            templates.map { t -> buildTemplate(t.id, t.name) }
        }

    override suspend fun getTemplate(id: Long): MealTemplate? {
        val t = templateDao.getById(id) ?: return null
        return buildTemplate(t.id, t.name)
    }

    private suspend fun buildTemplate(id: Long, name: String): MealTemplate {
        val itemEntities = templateDao.getItems(id)
        // Batch-resolve foods in one query (no N+1), tolerate a missing/archived food by skipping it.
        val foods = foodDao.getByIds(itemEntities.map { it.foodId }.distinct())
            .associateBy { it.id }
        val items = itemEntities.mapNotNull { ie ->
            val food = foods[ie.foodId] ?: return@mapNotNull null
            MealTemplateItem(food = food.toDomain(), amountG = ie.amountG)
        }
        return MealTemplate(id = id, name = name, items = items)
    }

    override suspend fun saveTemplate(id: Long, name: String, items: List<Pair<Long, Float>>): Long =
        database.withTransaction {
            val templateId = templateDao.upsertTemplate(
                MealTemplateEntity(id = id, name = name.trim())
            ).let { if (id > 0) id else it }
            templateDao.deleteItemsForTemplate(templateId)
            items.forEach { (foodId, amountG) ->
                templateDao.insertItem(
                    MealTemplateItemEntity(templateId = templateId, foodId = foodId, amountG = amountG)
                )
            }
            templateId
        }

    override suspend fun deleteTemplate(id: Long) {
        val t = templateDao.getById(id) ?: return
        templateDao.deleteTemplate(t)  // items cascade
    }

    override suspend fun applyToMeal(templateId: Long, date: String, mealType: MealType) {
        val template = getTemplate(templateId) ?: return
        if (template.items.isEmpty()) return
        val entryId = mealRepository.getOrCreateMealEntry(date, mealType)
        template.items.forEach { item ->
            mealRepository.addItemToMeal(
                mealEntryId = entryId,
                item = MealItem(
                    mealEntryId = entryId,
                    food = item.food,
                    amountG = item.amountG,
                    kcal = item.kcal,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                ),
            )
        }
    }
}

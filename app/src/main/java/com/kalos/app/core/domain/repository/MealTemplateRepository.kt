package com.kalos.app.core.domain.repository

import com.kalos.app.core.domain.model.MealTemplate
import com.kalos.app.core.domain.model.MealType
import kotlinx.coroutines.flow.Flow

interface MealTemplateRepository {
    fun getTemplates(): Flow<List<MealTemplate>>
    suspend fun getTemplate(id: Long): MealTemplate?

    /**
     * Creates or updates a favourite meal. [items] are (foodId, amountG) pairs.
     * @return the template id.
     */
    suspend fun saveTemplate(id: Long, name: String, items: List<Pair<Long, Float>>): Long

    suspend fun deleteTemplate(id: Long)

    /**
     * Applies a favourite to a meal: appends its items to the given date + meal type,
     * with macros computed like any manual add. Does not touch existing items.
     */
    suspend fun applyToMeal(templateId: Long, date: String, mealType: MealType)
}

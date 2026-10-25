package com.kalos.app

import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.model.passes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryFilterTest {
    private fun food(name: String, category: String = "Divers", tags: List<String> = emptyList()) = Food(
        name = name, category = category, kcalPer100g = 100f, proteinPer100g = 0f,
        carbsPer100g = 0f, fatPer100g = 0f, tags = tags,
    )

    @Test
    fun noPorkExcludesByNameAndTag() {
        val f = setOf(DietaryFilter.NO_PORK)
        assertFalse(food("Jambon blanc", "Viandes").passes(f))
        assertFalse(food("Quiche", tags = listOf("pork")).passes(f))
        assertTrue(food("Poulet rôti", "Viandes").passes(f))
    }

    @Test
    fun noAlcoholDoesNotMatchVinaigre() {
        val f = setOf(DietaryFilter.NO_ALCOHOL)
        assertFalse(food("Vin rouge", "Boissons").passes(f))
        assertFalse(food("Bière blonde", "Boissons").passes(f))
        assertTrue(food("Vinaigre balsamique", "Condiments").passes(f))
    }

    @Test
    fun vegetarianAndVeganUseCategoriesUnlessTagged() {
        assertFalse(food("Saumon", "Poissons").passes(setOf(DietaryFilter.VEGETARIAN)))
        assertTrue(food("Lentilles", "Légumineuses").passes(setOf(DietaryFilter.VEGETARIAN)))
        assertFalse(food("Yaourt nature", "Œufs & Laitages").passes(setOf(DietaryFilter.VEGAN)))
        assertTrue(food("Steak végétal", "Viandes", listOf("vegetarian", "vegan")).passes(setOf(DietaryFilter.VEGAN)))
    }

    @Test
    fun noFilterKeepsEverything() {
        assertTrue(food("Jambon", "Viandes").passes(emptySet()))
    }
}

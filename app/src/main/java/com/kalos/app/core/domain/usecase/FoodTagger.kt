package com.kalos.app.core.domain.usecase

import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.domain.model.Food

object FoodTagger {

    // Word-boundary regex to avoid "vinaigre" matching "vin", "vinaigrette" matching "vin", etc.
    private val PORK_RE = Regex(
        """\b(porc|jambon|bacon|lardons?|saucisson|andouillette|chipolata|chorizo|mortadelle|prosciutto|cochon)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val ALCOHOL_RE = Regex(
        """\b(bière|biere|vin\b|vin rouge|vin blanc|alcool|champagne|whisky|rhum|vodka|cidre)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val JUNK_KEYWORDS = listOf("frites", "pizza", "hamburger", "kebab", "chips", "hot dog", "nuggets")

    fun containsPork(food: Food): Boolean = PORK_RE.containsMatchIn(food.name)

    fun containsAlcohol(food: Food): Boolean = ALCOHOL_RE.containsMatchIn(food.name)

    fun isMeat(food: Food): Boolean = food.category == "Viandes"

    fun isFish(food: Food): Boolean = food.category == "Poissons"

    fun isAnimalProduct(food: Food): Boolean =
        isMeat(food) || isFish(food) || food.category == "Œufs & Laitages"

    /**
     * Nutritional quality tier used to adjust suggestion ranking:
     *  1 = whole / minimally processed food (fruits, vegetables, lean protein, legumes)
     *  2 = acceptable processed food (whole grains, dairy, fish, lean meat)
     *  3 = ultra-processed or junk food (fast food, sugary items, high-fat charcuterie, alcohol)
     */
    fun qualityTier(food: Food): Int {
        val lower = food.name.lowercase()

        // Tier 3: junk food by name
        if (JUNK_KEYWORDS.any { lower.contains(it) }) return 3
        // Tier 3: alcoholic beverages
        if (containsAlcohol(food)) return 3
        // Tier 3: ultra-processed / very high-fat charcuterie
        if (food.category == "Viandes" && food.fatPer100g > 40f) return 3
        // Tier 3: sugary items (sweets, pastries)
        if (food.category == "Sucreries") return 3

        // Tier 1: whole plant foods
        if (food.category in listOf("Fruits", "Légumes", "Légumineuses", "Oléagineux")) return 1
        // Tier 1: basic fish (not breaded, not rillettes)
        if (food.category == "Poissons" && !lower.contains("pané") && !lower.contains("rillettes")) return 1
        // Tier 1: basic eggs and low-calorie dairy
        if (food.category == "Œufs & Laitages" && food.kcalPer100g < 200f) return 1
        // Tier 1: lean unprocessed meat
        if (food.category == "Viandes" && food.kcalPer100g < 220f && food.fatPer100g < 12f
            && !lower.contains("sauci") && !lower.contains("chipo")) return 1
        // Tier 1: whole grains and basic starches
        if (food.category == "Céréales & Féculents" && food.kcalPer100g < 160f
            && !lower.contains("sandwich") && !lower.contains("crêpe") && !lower.contains("gaufre")
            && !lower.contains("pain perdu") && !lower.contains("gratin")) return 1

        return 2
    }

    fun qualityMultiplier(food: Food): Float = when (qualityTier(food)) {
        1 -> 1.0f
        2 -> 0.80f
        else -> 0.35f  // tier 3: heavily deprioritised but not removed
    }

    fun passes(food: Food, filters: Set<DietaryFilter>): Boolean {
        if (DietaryFilter.NO_PORK in filters && containsPork(food)) return false
        if (DietaryFilter.NO_ALCOHOL in filters && containsAlcohol(food)) return false
        if (DietaryFilter.VEGETARIAN in filters && (isMeat(food) || isFish(food))) return false
        if (DietaryFilter.VEGAN in filters && isAnimalProduct(food)) return false
        return true
    }
}

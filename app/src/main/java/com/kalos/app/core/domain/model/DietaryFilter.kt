package com.kalos.app.core.domain.model

enum class DietaryFilter(
    val label: String,
    val description: String,
) {
    NO_PORK(
        "Sans porc",
        "Exclut porc, bacon, jambon, lardons et charcuteries porcines",
    ),
    NO_ALCOHOL(
        "Sans alcool",
        "Exclut bière, vin et boissons alcoolisées",
    ),
    VEGETARIAN(
        "Végétarien",
        "Exclut viande et poisson",
    ),
    VEGAN(
        "Végan",
        "Exclut tout produit animal (viande, poisson, laitages, œufs)",
    ),
}

// Word boundaries so "vinaigre" does not match "vin".
private val PORK_RE = Regex(
    """\b(porc|jambon|bacon|lardons?|saucisson|andouillette|chipolata|chorizo|mortadelle|prosciutto|cochon)\b""",
    RegexOption.IGNORE_CASE,
)
private val ALCOHOL_RE = Regex(
    """\b(bières?|bieres?|vin|vins|alcool|champagne|whisky|rhum|vodka|cidre)\b""",
    RegexOption.IGNORE_CASE,
)

/** Whether [this] food is allowed by every active filter. Explicit user tags take precedence. */
fun Food.passes(filters: Set<DietaryFilter>): Boolean {
    if (filters.isEmpty()) return true
    val meatOrFish = category == "Viandes" || category == "Poissons"
    return when {
        DietaryFilter.NO_PORK in filters && ("pork" in tags || PORK_RE.containsMatchIn(name)) -> false
        DietaryFilter.NO_ALCOHOL in filters && ("alcohol" in tags || ALCOHOL_RE.containsMatchIn(name)) -> false
        DietaryFilter.VEGETARIAN in filters && "vegetarian" !in tags && meatOrFish -> false
        DietaryFilter.VEGAN in filters && "vegan" !in tags && (meatOrFish || category == "Œufs & Laitages") -> false
        else -> true
    }
}

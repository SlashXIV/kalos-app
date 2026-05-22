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

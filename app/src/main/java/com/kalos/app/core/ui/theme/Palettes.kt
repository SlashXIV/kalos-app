package com.kalos.app.core.ui.theme

import com.kalos.app.core.ui.theme.palettes.EncrePalette
import com.kalos.app.core.ui.theme.palettes.JadePalette
import com.kalos.app.core.ui.theme.palettes.OceanPalette

object Palettes {
    val all: List<KalosPalette> = listOf(JadePalette, EncrePalette, OceanPalette)
    val default: KalosPalette = JadePalette

    /** Identifiant inconnu ou absent → palette par défaut, jamais d'écran sans thème. */
    fun byId(id: String?): KalosPalette = all.firstOrNull { it.id == id } ?: default
}

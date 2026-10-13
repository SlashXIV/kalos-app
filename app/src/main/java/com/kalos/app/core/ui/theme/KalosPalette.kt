package com.kalos.app.core.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable

/**
 * Une palette de Kalos, en clair et en sombre.
 * Ajouter une palette = écrire un fichier dans `palettes/` et l'inscrire dans [Palettes.all] ;
 * `PaletteContrastTest` vérifie alors automatiquement sa lisibilité.
 */
@Immutable
data class KalosPalette(
    /** Identifiant persisté dans les réglages : ne jamais le renommer. */
    val id: String,
    @StringRes val nameRes: Int,
    val light: ColorScheme,
    val dark: ColorScheme,
)

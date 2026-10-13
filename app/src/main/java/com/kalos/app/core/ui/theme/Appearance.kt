package com.kalos.app.core.ui.theme

/** Luminosité choisie, indépendante de la palette. */
enum class Brightness { SYSTEM, LIGHT, DARK }

/** Réglage « Ambiance » : Expressive (tout le mouvement) ou Sobre (strict utile). */
enum class MotionPreference { EXPRESSIVE, CALM }

data class AppearanceSettings(
    val paletteId: String = Palettes.default.id,
    val brightness: Brightness = Brightness.SYSTEM,
    val motion: MotionPreference = MotionPreference.EXPRESSIVE,
    val vibrations: Boolean = true,
)

fun Brightness.isDark(systemInDark: Boolean): Boolean = when (this) {
    Brightness.SYSTEM -> systemInDark
    Brightness.LIGHT -> false
    Brightness.DARK -> true
}

/**
 * Thèmes des versions 3.x (un seul réglage mêlant palette et luminosité) → palette + luminosité.
 * Une palette retirée retombe sur la palette par défaut en gardant sa luminosité d'origine.
 */
fun migrateLegacyTheme(name: String): Pair<String, Brightness> = when (name) {
    "LIGHT" -> "jade" to Brightness.LIGHT
    "DARK" -> "jade" to Brightness.DARK
    "OCEAN" -> "ocean" to Brightness.DARK
    "MONOCHROME" -> "encre" to Brightness.DARK
    "PASTEL" -> "jade" to Brightness.LIGHT
    "BERRY", "AURORA", "AQUA", "SUNSET" -> "jade" to Brightness.DARK
    else -> "jade" to Brightness.SYSTEM // SYSTEM ou valeur inconnue
}

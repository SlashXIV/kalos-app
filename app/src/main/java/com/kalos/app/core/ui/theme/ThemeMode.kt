package com.kalos.app.core.ui.theme

/**
 * User-selectable theme. SYSTEM/LIGHT/DARK use the default emerald palette; the named
 * entries are distinct colour palettes (each inherently light or dark).
 */
enum class ThemeMode(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair"),
    DARK("Sombre"),
    PASTEL("Pastel"),
    BERRY("Berry"),
    AURORA("Aurora"),
    MONOCHROME("Monochrome"),
    AQUA("Aqua"),
    OCEAN("Océan"),
    SUNSET("Sunset"),
}

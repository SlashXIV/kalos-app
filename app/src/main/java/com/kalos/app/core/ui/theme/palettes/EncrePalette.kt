package com.kalos.app.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.kalos.app.R
import com.kalos.app.core.ui.theme.Accents
import com.kalos.app.core.ui.theme.KalosPalette
import com.kalos.app.core.ui.theme.darkScheme
import com.kalos.app.core.ui.theme.lightScheme

/** Monochrome : l'accent est l'encre elle-même. Remplace « Monochrome ». */
val EncrePalette = KalosPalette(
    id = "encre",
    nameRes = R.string.palette_encre,
    light = lightScheme(Accents(Color(0xFF1B1F23), Color(0xFFFFFFFF), Color(0xFFE2E4E1), Color(0xFF14171A))),
    dark = darkScheme(Accents(Color(0xFFE8EAE6), Color(0xFF121417), Color(0xFF2A2E33), Color(0xFFECEEEA))),
)

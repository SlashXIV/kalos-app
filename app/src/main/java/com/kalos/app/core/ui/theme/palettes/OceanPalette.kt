package com.kalos.app.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.kalos.app.R
import com.kalos.app.core.ui.theme.Accents
import com.kalos.app.core.ui.theme.KalosPalette
import com.kalos.app.core.ui.theme.darkScheme
import com.kalos.app.core.ui.theme.lightScheme

/** Bleu profond, héritière d'« Océan ». */
val OceanPalette = KalosPalette(
    id = "ocean",
    nameRes = R.string.palette_ocean,
    light = lightScheme(Accents(Color(0xFF1F5FA0), Color(0xFFFFFFFF), Color(0xFFD6E6F6), Color(0xFF0B2F55))),
    dark = darkScheme(Accents(Color(0xFF7DB5EA), Color(0xFF0A2340), Color(0xFF13355A), Color(0xFFCFE4FA))),
)

package com.kalos.app.core.ui.theme.palettes

import androidx.compose.ui.graphics.Color
import com.kalos.app.R
import com.kalos.app.core.ui.theme.Accents
import com.kalos.app.core.ui.theme.KalosPalette
import com.kalos.app.core.ui.theme.darkScheme
import com.kalos.app.core.ui.theme.lightScheme

/** Palette par défaut, héritière de l'émeraude des versions 3.x. */
val JadePalette = KalosPalette(
    id = "jade",
    nameRes = R.string.palette_jade,
    light = lightScheme(Accents(Color(0xFF0B7A57), Color(0xFFFFFFFF), Color(0xFFD3EDE3), Color(0xFF053D2B))),
    dark = darkScheme(Accents(Color(0xFF3CCB98), Color(0xFF062A1D), Color(0xFF0F3A2C), Color(0xFFA6F0D2))),
)

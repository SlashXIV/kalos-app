package com.kalos.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Rayons nommés par usage. Peu d'arrondi : un instrument, pas une bulle. */
object KalosShape {
    val chip = RoundedCornerShape(4.dp)
    val control = RoundedCornerShape(6.dp)
    val card = RoundedCornerShape(10.dp)
    val sheet = RoundedCornerShape(14.dp)
}

internal val KalosShapes = Shapes(
    extraSmall = KalosShape.chip,
    small = KalosShape.control,
    medium = KalosShape.card,
    large = KalosShape.sheet,
    extraLarge = KalosShape.sheet,
)

@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.kalos.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kalos.app.R

// Polices variables embarquées (OFL, licences dans assets/licenses) : une graisse = un réglage d'axe.
private fun variable(res: Int, weight: Int, width: Float? = null) = Font(
    res,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        *listOfNotNull(FontVariation.weight(weight), width?.let { FontVariation.width(it) }).toTypedArray(),
    ),
)

/** Affiche : plaques d'instrument, titres d'écran. */
val BigShoulders = FontFamily(variable(R.font.big_shoulders_display, 700), variable(R.font.big_shoulders_display, 800))
/** Texte courant. */
val Hanken = FontFamily(
    variable(R.font.hanken_grotesk, 400), variable(R.font.hanken_grotesk, 500),
    variable(R.font.hanken_grotesk, 600), variable(R.font.hanken_grotesk, 700),
)
/** Chiffres : chasse fixe, légèrement condensée, pour que kcal, grammes et chronos ne « dansent » pas. */
val Martian = FontFamily(variable(R.font.martian_mono, 400, 87.5f), variable(R.font.martian_mono, 500, 87.5f))

private val Tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/** Styles propres à Kalos, nommés par usage (et non par taille). */
@Immutable
data class KalosType(
    /** Titre d'écran principal (« AUJOURD'HUI »), en capitales. */
    val poster: TextStyle,
    /** Titre d'écran secondaire. */
    val posterSmall: TextStyle,
    /** La valeur du jour (kcal au centre du cadran). */
    val metricHuge: TextStyle,
    val metricLarge: TextStyle,
    /** Grammes, charges, chronos dans une ligne. */
    val metric: TextStyle,
    val metricSmall: TextStyle,
    /** Étiquette capitale espacée, façon plaque d'instrument. */
    val label: TextStyle,
    /** Bouton d'action principal. */
    val action: TextStyle,
)

internal val DefaultKalosType = KalosType(
    poster = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 0.95.em, letterSpacing = 0.01.em, lineHeightStyle = Tight),
    posterSmall = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 1.em, letterSpacing = 0.01.em, lineHeightStyle = Tight),
    metricHuge = TextStyle(fontFamily = Martian, fontWeight = FontWeight.Medium, fontSize = 44.sp, lineHeight = 1.em, letterSpacing = (-0.02).em, lineHeightStyle = Tight),
    metricLarge = TextStyle(fontFamily = Martian, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 1.1.em, letterSpacing = (-0.02).em),
    metric = TextStyle(fontFamily = Martian, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 1.3.em, letterSpacing = (-0.01).em),
    metricSmall = TextStyle(fontFamily = Martian, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 1.3.em),
    label = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 1.3.em, letterSpacing = 0.15.em),
    action = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 1.2.em, letterSpacing = 0.06.em),
)

internal val LocalKalosType = staticCompositionLocalOf { DefaultKalosType }

/** Échelle Material pour les composants qui la lisent (barres, champs, dialogues). */
internal val KalosMaterialTypography = Typography(
    displayLarge = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = BigShoulders, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 0.01.em),
    titleMedium = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.02.em),
    labelMedium = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = Hanken, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
)

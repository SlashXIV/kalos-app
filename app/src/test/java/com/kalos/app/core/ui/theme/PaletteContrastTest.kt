package com.kalos.app.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Lisibilité de CHAQUE palette, en clair et en sombre : texte 7:1, accents 4,5:1, couleurs de
 * nutriments 3:1, et nutriments distincts deux à deux pour les daltoniens (protan, deutan).
 * Ajouter une palette au registre suffit pour qu'elle soit vérifiée ici.
 */
class PaletteContrastTest {

    private val variants: List<Triple<String, ColorScheme, NutrientColors>> = Palettes.all.flatMap {
        listOf(
            Triple("${it.id}/clair", it.light, NutrientColors.Light),
            Triple("${it.id}/sombre", it.dark, NutrientColors.Dark),
        )
    }

    @Test
    fun `le texte atteint 7 pour 1`() = check(7.0) { s, _ ->
        listOf(
            "onBackground/background" to (s.onBackground to s.background),
            "onSurface/surfaceContainer" to (s.onSurface to s.surfaceContainer),
        )
    }

    @Test
    fun `le texte secondaire et les accents atteignent 4,5 pour 1`() = check(4.5) { s, n ->
        listOf(
            "onSurfaceVariant/background" to (s.onSurfaceVariant to s.background),
            "onSurfaceVariant/surfaceContainer" to (s.onSurfaceVariant to s.surfaceContainer),
            "onPrimary/primary" to (s.onPrimary to s.primary),
            "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
            // L'accent sert aussi de couleur de texte (liens, valeurs mises en avant).
            "primary/background" to (s.primary to s.background),
            "primary/surfaceContainer" to (s.primary to s.surfaceContainer),
            "onError/error" to (s.onError to s.error),
            // Les états servent de texte (« objectif atteint », « au-dessus »).
            "goalReached/background" to (n.goalReached to s.background),
            "overTarget/background" to (n.overTarget to s.background),
        )
    }

    @Test
    fun `les couleurs de graphiques atteignent 3 pour 1`() = check(3.0) { s, n ->
        listOf(
            "protein" to (n.protein to s.background), "carbs" to (n.carbs to s.background),
            "fat" to (n.fat to s.background), "water" to (n.water to s.background),
            "protein/carte" to (n.protein to s.surfaceContainer), "carbs/carte" to (n.carbs to s.surfaceContainer),
            "fat/carte" to (n.fat to s.surfaceContainer), "water/carte" to (n.water to s.surfaceContainer),
            "outline/background" to (s.outline to s.background),
        )
    }

    @Test
    fun `les nutriments restent distincts pour les daltoniens`() {
        for ((name, _, n) in variants) {
            val colors = listOf("protein" to n.protein, "carbs" to n.carbs, "fat" to n.fat, "water" to n.water)
            for (i in colors.indices) for (j in i + 1 until colors.size) {
                for ((cvd, m) in listOf("protan" to PROTAN, "deutan" to DEUTAN)) {
                    val d = deltaE(simulate(colors[i].second, m), simulate(colors[j].second, m))
                    assertTrue("$name ${colors[i].first}/${colors[j].first} $cvd : ΔE %.1f, minimum 8".format(d), d >= 8.0)
                }
            }
        }
    }

    private fun check(min: Double, pairs: (ColorScheme, NutrientColors) -> List<Pair<String, Pair<Color, Color>>>) {
        for ((name, scheme, nutrients) in variants) for ((pair, colors) in pairs(scheme, nutrients)) {
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$name : $pair = %.2f:1, minimum $min:1".format(ratio), ratio >= min)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(c: Color): Double =
        0.2126 * srgbToLinear(c.red) + 0.7152 * srgbToLinear(c.green) + 0.0722 * srgbToLinear(c.blue)

    private fun srgbToLinear(v: Float): Double = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun simulate(c: Color, m: Array<DoubleArray>): DoubleArray {
        val rgb = doubleArrayOf(srgbToLinear(c.red), srgbToLinear(c.green), srgbToLinear(c.blue))
        return DoubleArray(3) { i -> (m[i][0] * rgb[0] + m[i][1] * rgb[1] + m[i][2] * rgb[2]).coerceIn(0.0, 1.0) }
    }

    /** Distance OKLab × 100, comme le validateur de palettes. */
    private fun deltaE(a: DoubleArray, b: DoubleArray): Double {
        val (la, lb) = oklab(a) to oklab(b)
        return 100 * sqrt((0..2).sumOf { (la[it] - lb[it]).pow(2) })
    }

    private fun oklab(rgb: DoubleArray): DoubleArray {
        val (r, g, b) = Triple(rgb[0], rgb[1], rgb[2])
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private companion object {
        // Machado, Oliveira & Fernandes 2009, sévérité 1 (RGB linéaire).
        val PROTAN = arrayOf(
            doubleArrayOf(0.152286, 1.052583, -0.204868),
            doubleArrayOf(0.114503, 0.786281, 0.099216),
            doubleArrayOf(-0.003882, -0.048116, 1.051998),
        )
        val DEUTAN = arrayOf(
            doubleArrayOf(0.367322, 0.860646, -0.227968),
            doubleArrayOf(0.280085, 0.672501, 0.047413),
            doubleArrayOf(-0.011820, 0.042940, 0.968881),
        )
    }
}

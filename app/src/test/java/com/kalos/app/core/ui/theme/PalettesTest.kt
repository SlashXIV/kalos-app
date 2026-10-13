package com.kalos.app.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PalettesTest {
    @Test
    fun `un identifiant absent ou inconnu retombe sur la palette par defaut`() {
        assertEquals(Palettes.default, Palettes.byId(null))
        assertEquals(Palettes.default, Palettes.byId("berry"))
    }

    @Test
    fun `chaque palette est retrouvee par son identifiant unique`() {
        Palettes.all.forEach { assertEquals(it, Palettes.byId(it.id)) }
        assertEquals(Palettes.all.size, Palettes.all.map { it.id }.toSet().size)
    }

    @Test
    fun `les anciens themes deviennent une palette existante et gardent leur luminosite`() {
        val expected = mapOf(
            "SYSTEM" to ("jade" to Brightness.SYSTEM), "LIGHT" to ("jade" to Brightness.LIGHT),
            "DARK" to ("jade" to Brightness.DARK), "OCEAN" to ("ocean" to Brightness.DARK),
            "MONOCHROME" to ("encre" to Brightness.DARK), "PASTEL" to ("jade" to Brightness.LIGHT),
            "BERRY" to ("jade" to Brightness.DARK), "AURORA" to ("jade" to Brightness.DARK),
            "AQUA" to ("jade" to Brightness.DARK), "SUNSET" to ("jade" to Brightness.DARK),
            "valeur inconnue" to ("jade" to Brightness.SYSTEM),
        )
        expected.forEach { (legacy, result) ->
            assertEquals(legacy, result, migrateLegacyTheme(legacy))
            // L'identifiant migré doit exister dans le registre, sinon le repli masquerait l'erreur.
            assertEquals(legacy, true, Palettes.all.any { it.id == result.first })
        }
    }
}

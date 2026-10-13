package com.kalos.app.core.ui.motion

import com.kalos.app.core.ui.theme.MotionPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionLevelTest {
    @Test
    fun `ambiance expressive donne tout le mouvement`() =
        assertEquals(MotionLevel.FULL, motionLevelOf(1f, MotionPreference.EXPRESSIVE))

    @Test
    fun `ambiance sobre coupe le decor mais garde les transitions`() {
        val level = motionLevelOf(1f, MotionPreference.CALM)
        assertEquals(MotionLevel.REDUCED, level)
        assertFalse(level.decorative)
        assertTrue(level.animates)
    }

    @Test
    fun `supprimer les animations prime sur l ambiance`() =
        assertEquals(MotionLevel.NONE, motionLevelOf(0f, MotionPreference.EXPRESSIVE))
}

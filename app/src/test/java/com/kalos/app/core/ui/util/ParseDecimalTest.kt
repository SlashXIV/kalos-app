package com.kalos.app.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseDecimalTest {
    @Test
    fun acceptsFrenchCommaAndDot() {
        assertEquals(82.5f, "82,5".parseDecimal())
        assertEquals(82.5f, "82.5".parseDecimal())
        assertEquals(100f, " 100 ".parseDecimal())
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull("".parseDecimal())
        assertNull("abc".parseDecimal())
        assertNull("1,2,3".parseDecimal())
    }
}

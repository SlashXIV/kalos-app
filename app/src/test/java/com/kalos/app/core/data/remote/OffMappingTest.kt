package com.kalos.app.core.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OffMappingTest {
    @Test
    fun kilojoulesOnlyAreConvertedToKcal() {
        val body = """{"status":1,"product":{"product_name":"Biscuit","nutriments":{"energy-kj_100g":2092}}}"""
        assertEquals(500f, decodeOffFood(body, "3017620422003")!!.kcalPer100g, 0.5f)
    }

    @Test
    fun servingQuantityAcceptsNumberOrString() {
        val asNumber = """{"status":1,"product":{"product_name":"Yaourt","serving_quantity":125,"nutriments":{"energy-kcal_100g":60}}}"""
        val asString = """{"status":1,"product":{"product_name":"Yaourt","serving_quantity":"125","nutriments":{"energy-kcal_100g":60}}}"""
        assertEquals(125f, decodeOffFood(asNumber, "1")!!.defaultServingG)
        assertEquals(125f, decodeOffFood(asString, "1")!!.defaultServingG)
    }

    @Test
    fun missingServingDefaultsTo100g() {
        val body = """{"status":1,"product":{"product_name":"Eau","nutriments":{"energy-kcal_100g":0}}}"""
        assertEquals(100f, decodeOffFood(body, "1")!!.defaultServingG)
    }

    @Test
    fun notFoundStatusGivesNull() {
        assertNull(decodeOffFood("""{"status":0}""", "1"))
    }
}

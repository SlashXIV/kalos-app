package com.kalos.app

import com.kalos.app.core.domain.model.RepRecord
import com.kalos.app.core.domain.model.meaningful
import com.kalos.app.core.domain.usecase.EstimateOneRepMaxUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepRecordsTest {
    private val estimate = EstimateOneRepMaxUseCase()

    @Test
    fun recordsBeatenByLongerSetsAreHidden() {
        val records = listOf(RepRecord(6, 22f), RepRecord(8, 20f), RepRecord(10, 24f), RepRecord(12, 18f), RepRecord(3, 30f))
        // 22 × 6 et 20 × 8 sont battus par 24 × 10 ; 18 × 12 reste (personne au-delà de 12).
        assertEquals(listOf(RepRecord(3, 30f), RepRecord(10, 24f), RepRecord(12, 18f)), records.meaningful())
    }

    @Test
    fun equalWeightWithMoreRepsWins() {
        assertEquals(listOf(RepRecord(10, 24f)), listOf(RepRecord(8, 24f), RepRecord(10, 24f)).meaningful())
    }

    @Test
    fun epleyEstimate() {
        assertEquals(32f, estimate(listOf(RepRecord(10, 24f)))!!, 0.01f) // 24 × (1 + 10/30)
        assertEquals(30f, estimate(listOf(RepRecord(1, 30f)))!!, 0.01f) // une répétition : la charge elle-même
        assertEquals(32f, estimate(listOf(RepRecord(1, 30f), RepRecord(10, 24f)))!!, 0.01f) // la meilleure estimation
    }

    @Test
    fun longSetsDoNotEstimate() {
        assertNull(estimate(listOf(RepRecord(15, 40f))))
        assertNull(estimate(emptyList()))
    }
}

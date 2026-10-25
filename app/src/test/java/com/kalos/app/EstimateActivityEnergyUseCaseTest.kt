package com.kalos.app

import com.kalos.app.core.domain.model.ActivityType
import com.kalos.app.core.domain.usecase.EstimateActivityEnergyUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateActivityEnergyUseCaseTest {
    private val estimate = EstimateActivityEnergyUseCase()

    @Test
    fun metTimesWeightTimesHours() {
        // 9.8 MET × 70 kg × 1 h = 686 kcal
        assertEquals(686, estimate(ActivityType.RUNNING, 70f, 3600))
        // 7.5 MET × 80 kg × 2 h = 1200 kcal
        assertEquals(1200, estimate(ActivityType.CLIMBING, 80f, 7200))
    }

    @Test
    fun zeroDurationIsZero() {
        assertEquals(0, estimate(ActivityType.YOGA, 70f, 0))
    }
}

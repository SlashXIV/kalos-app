package com.kalos.app

import com.kalos.app.core.domain.model.ActivityLevel
import com.kalos.app.core.domain.model.FitnessGoal
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateBmrUseCaseTest {

    private lateinit var useCase: CalculateBmrUseCase

    @Before
    fun setUp() {
        useCase = CalculateBmrUseCase()
    }

    private fun profile(
        weightKg: Float = 80f,
        heightCm: Float = 180f,
        age: Int = 30,
        sex: Sex = Sex.MALE,
    ) = UserProfile(
        weightKg = weightKg, heightCm = heightCm, age = age, sex = sex,
        activityLevel = ActivityLevel.MODERATE, goal = FitnessGoal.MAINTAIN,
    )

    @Test
    fun `male BMR uses positive constant`() {
        // Male: 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val result = useCase(profile(sex = Sex.MALE))
        assertEquals(1780f, result, 0.1f)
    }

    @Test
    fun `female BMR uses negative constant`() {
        // Female: 10*80 + 6.25*180 - 5*30 - 161 = 800 + 1125 - 150 - 161 = 1614
        val result = useCase(profile(sex = Sex.FEMALE))
        assertEquals(1614f, result, 0.1f)
    }

    @Test
    fun `BMR increases with higher weight`() {
        val light = useCase(profile(weightKg = 60f))
        val heavy = useCase(profile(weightKg = 100f))
        assert(heavy > light)
    }

    @Test
    fun `BMR increases with greater height`() {
        val short = useCase(profile(heightCm = 160f))
        val tall = useCase(profile(heightCm = 200f))
        assert(tall > short)
    }

    @Test
    fun `BMR decreases with older age`() {
        val young = useCase(profile(age = 20))
        val old = useCase(profile(age = 60))
        assert(young > old)
    }
}

package com.kalos.app

import com.kalos.app.core.domain.model.ActivityLevel
import com.kalos.app.core.domain.model.FitnessGoal
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateTdeeUseCaseTest {

    private lateinit var useCase: CalculateTdeeUseCase

    @Before
    fun setUp() {
        useCase = CalculateTdeeUseCase(CalculateBmrUseCase())
    }

    private fun profile(
        activityLevel: ActivityLevel = ActivityLevel.MODERATE,
        sex: Sex = Sex.MALE,
        weightKg: Float = 80f,
        heightCm: Float = 180f,
        age: Int = 30,
    ) = UserProfile(
        weightKg = weightKg, heightCm = heightCm, age = age, sex = sex,
        activityLevel = activityLevel, goal = FitnessGoal.MAINTAIN,
    )

    @Test
    fun `sedentary TDEE equals BMR times 1_2`() {
        // BMR male 80kg 180cm 30yo = 1780
        val tdee = useCase(profile(ActivityLevel.SEDENTARY))
        assertEquals(1780f * 1.2f, tdee, 1f)
    }

    @Test
    fun `moderate TDEE equals BMR times 1_55`() {
        val tdee = useCase(profile(ActivityLevel.MODERATE))
        assertEquals(1780f * 1.55f, tdee, 1f)
    }

    @Test
    fun `very active TDEE equals BMR times 1_9`() {
        val tdee = useCase(profile(ActivityLevel.VERY_ACTIVE))
        assertEquals(1780f * 1.9f, tdee, 1f)
    }

    @Test
    fun `higher activity produces higher TDEE`() {
        val sedentary = useCase(profile(ActivityLevel.SEDENTARY))
        val veryActive = useCase(profile(ActivityLevel.VERY_ACTIVE))
        assert(veryActive > sedentary)
    }
}

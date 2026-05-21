package com.kalos.app

import com.kalos.app.core.domain.model.ActivityLevel
import com.kalos.app.core.domain.model.FitnessGoal
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

class CalculateMacroGoalsUseCaseTest {

    private lateinit var useCase: CalculateMacroGoalsUseCase

    @Before
    fun setUp() {
        val bmrUseCase = CalculateBmrUseCase()
        val tdeeUseCase = CalculateTdeeUseCase(bmrUseCase)
        useCase = CalculateMacroGoalsUseCase(tdeeUseCase)
    }

    private fun profile(
        weightKg: Float = 80f,
        goal: FitnessGoal = FitnessGoal.MAINTAIN,
        activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    ) = UserProfile(
        weightKg = weightKg, heightCm = 180f, age = 30,
        sex = Sex.MALE, activityLevel = activityLevel, goal = goal,
    )

    @Test
    fun `protein is 2g per kg body weight`() {
        val result = useCase(profile(weightKg = 80f))
        assertEquals(160, result.proteinG)
    }

    @Test
    fun `fat is approximately 25 percent of target calories`() {
        val result = useCase(profile())
        val fatKcal = result.fatG * 9f
        val fatPercent = fatKcal / result.kcal
        assertEquals(0.25f, fatPercent, 0.02f)
    }

    @Test
    fun `deficit goal produces lower calorie target than maintain`() {
        val maintain = useCase(profile(goal = FitnessGoal.MAINTAIN))
        val deficit = useCase(profile(goal = FitnessGoal.LOSE_FAST))
        assertTrue(deficit.kcal < maintain.kcal)
    }

    @Test
    fun `surplus goal produces higher calorie target than maintain`() {
        val maintain = useCase(profile(goal = FitnessGoal.MAINTAIN))
        val surplus = useCase(profile(goal = FitnessGoal.GAIN_FAST))
        assertTrue(surplus.kcal > maintain.kcal)
    }

    @Test
    fun `minimum calorie target is 1200`() {
        val extreme = profile(weightKg = 40f, goal = FitnessGoal.LOSE_FAST, activityLevel = ActivityLevel.SEDENTARY)
        val result = useCase(extreme)
        assertTrue(result.kcal >= 1200)
    }

    @Test
    fun `macro calories approximately sum to target calories`() {
        val result = useCase(profile())
        val macroSum = result.proteinG * 4 + result.carbsG * 4 + result.fatG * 9
        // Allow ±50 kcal due to rounding
        assertTrue(kotlin.math.abs(macroSum - result.kcal) < 50)
    }
}

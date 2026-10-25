package com.kalos.app

import com.kalos.app.core.domain.model.KcalZone
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.domain.model.completedVolumeKg
import com.kalos.app.core.ui.util.todayFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DomainRulesTest {
    private fun set(reps: Int, kg: Float, done: Boolean) =
        WorkoutSet(logExerciseId = 1, setNumber = 1, reps = reps, weightKg = kg, isCompleted = done)

    @Test
    fun volumeCountsCompletedSetsOnly() {
        val sets = listOf(set(10, 82.5f, true), set(8, 80f, true), set(10, 100f, false), set(0, 0f, true))
        assertEquals(1465f, sets.completedVolumeKg())
    }

    @Test
    fun programWorkoutOnMatchesWeekday() {
        val monday = ProgramWorkout(programId = 1, template = null, dayOfWeek = 1)
        val program = TrainingProgram(name = "PPL", workouts = listOf(monday))
        assertEquals(monday, program.workoutOn(1))
        assertNull(program.workoutOn(2))
    }

    @Test
    fun todayFlowStartsWithToday() = runBlocking {
        assertEquals(LocalDate.now(), todayFlow().first())
    }

    @Test
    fun kcalZoneFollowsTheGoalBand() {
        val goal = NutritionGoal(kcal = 2000)
        assertEquals(KcalZone.BELOW, goal.zoneOf(1799f))
        assertEquals(KcalZone.IN, goal.zoneOf(1800f))
        assertEquals(KcalZone.IN, goal.zoneOf(2100f))
        assertEquals(KcalZone.OVER, goal.zoneOf(2101f))
        assertEquals(KcalZone.BELOW, NutritionGoal(kcal = 0).zoneOf(500f))
    }
}

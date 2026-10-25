package com.kalos.app.feature.workout.history

import com.kalos.app.core.domain.model.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyVolumesTest {
    private fun log(date: String, kg: Float) = WorkoutLog(date = date, startedAt = 0, totalVolumeKg = kg)

    @Test
    fun weeksWithoutSessionAreZeroNotSkipped() {
        // Lundi 5 oct. puis lundi 19 oct. 2026 : la semaine du 12 doit apparaître à zéro.
        val weeks = computeWeeklyVolumes(listOf(log("2026-10-05", 100f), log("2026-10-08", 50f), log("2026-10-21", 200f)))
        assertEquals(listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19)), weeks.map { it.weekStart })
        assertEquals(listOf(150f, 0f, 200f), weeks.map { it.volumeKg })
    }

    @Test
    fun keepsTheLastEightWeeksAndIgnoresActivities() {
        val logs = (0L..11L).map { log(LocalDate.of(2026, 1, 5).plusWeeks(it).toString(), 10f) } + log("2026-03-31", 0f)
        val weeks = computeWeeklyVolumes(logs)
        assertEquals(8, weeks.size)
        assertEquals(LocalDate.of(2026, 3, 23), weeks.last().weekStart)
    }

    @Test
    fun noVolumeNoWeeks() {
        assertEquals(emptyList<WeeklyVolume>(), computeWeeklyVolumes(listOf(log("2026-10-05", 0f))))
    }
}

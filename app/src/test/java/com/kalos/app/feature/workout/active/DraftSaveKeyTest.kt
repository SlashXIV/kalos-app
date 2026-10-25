package com.kalos.app.feature.workout.active

import com.kalos.app.feature.workout.active.ActiveWorkoutViewModel.Companion.draftSaveKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DraftSaveKeyTest {
    private val base = ActiveWorkoutUiState(templateName = "Push", isLoading = false, elapsedSecs = 60)

    @Test
    fun timerTicksDoNotTriggerASave() {
        assertEquals(draftSaveKey(base), draftSaveKey(base.copy(elapsedSecs = 61)))
        assertEquals(draftSaveKey(base), draftSaveKey(base.copy(restSecsLeft = 42)))
    }

    @Test
    fun heartbeatEvery30Seconds() {
        assertNotEquals(draftSaveKey(base), draftSaveKey(base.copy(elapsedSecs = 90)))
    }

    @Test
    fun realEditsTriggerASave() {
        assertNotEquals(draftSaveKey(base), draftSaveKey(base.copy(currentExIndex = 1)))
        assertNotEquals(draftSaveKey(base), draftSaveKey(base.copy(isResting = true)))
    }
}

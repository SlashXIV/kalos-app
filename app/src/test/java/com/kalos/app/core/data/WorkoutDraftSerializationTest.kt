package com.kalos.app.core.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutDraftSerializationTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun exercise(trackingMode: String) = ExerciseDraft(
        exerciseId = 1, exerciseName = "Gainage", exercisePrimaryMuscle = "Abdominaux",
        templateExId = 1, orderIndex = 0, defaultSets = 3, defaultReps = 0, defaultWeightKg = 0f,
        restSeconds = 60, notes = "", sets = listOf(SetDraft(reps = "", weight = "", isCompleted = true, duration = "1:00")),
        trackingMode = trackingMode,
    )

    @Test
    fun trackingModeSurvivesRoundTrip() {
        val draft = WorkoutDraft(templateId = 1, templateName = "Core", startedAt = 0, exercises = listOf(exercise("DURATION")))
        val decoded = json.decodeFromString(WorkoutDraft.serializer(), json.encodeToString(WorkoutDraft.serializer(), draft))
        assertEquals("DURATION", decoded.exercises.single().trackingMode)
    }

    @Test
    fun legacyDraftWithoutTrackingModeStillDecodes() {
        val legacy = """{"templateId":1,"templateName":"Core","startedAt":0,"exercises":[{"exerciseId":1,
            "exerciseName":"Gainage","exercisePrimaryMuscle":"Abdominaux","templateExId":1,"orderIndex":0,
            "defaultSets":3,"defaultReps":0,"defaultWeightKg":0.0,"restSeconds":60,"notes":"","sets":[]}]}"""
        assertEquals("", json.decodeFromString(WorkoutDraft.serializer(), legacy).exercises.single().trackingMode)
    }
}

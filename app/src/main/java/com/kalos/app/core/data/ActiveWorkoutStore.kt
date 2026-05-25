package com.kalos.app.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkoutDraft(
    val templateId: Long,
    val templateName: String,
    val startedAt: Long,
    val currentExIndex: Int = 0,
    val restStartedAt: Long? = null,
    val restDurationSecs: Int = 0,
    val exercises: List<ExerciseDraft>,
)

@Serializable
data class ExerciseDraft(
    val exerciseId: Long,
    val exerciseName: String,
    val exercisePrimaryMuscle: String,
    val templateExId: Long,
    val orderIndex: Int,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float,
    val restSeconds: Int,
    val notes: String,
    val sets: List<SetDraft>,
    val status: String = "PLANNED",
    val originalExerciseName: String = "",
)

@Serializable
data class SetDraft(
    val reps: String,
    val weight: String,
    val isCompleted: Boolean,
)

@Singleton
class ActiveWorkoutStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("kalos_active_workout", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(draft: WorkoutDraft) {
        prefs.edit().putString(KEY, json.encodeToString(WorkoutDraft.serializer(), draft)).apply()
    }

    fun load(): WorkoutDraft? {
        val raw = prefs.getString(KEY, null) ?: return null
        return try { json.decodeFromString(WorkoutDraft.serializer(), raw) } catch (_: Exception) { null }
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    companion object {
        private const val KEY = "draft"
        const val EXPIRY_MS = 24 * 60 * 60 * 1000L
    }
}

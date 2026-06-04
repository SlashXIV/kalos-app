package com.kalos.app.core.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkoutDraft(
    val templateId: Long,
    val templateName: String,
    val startedAt: Long,
    // Wall-clock of the last auto-save (~400ms cadence while the session is active).
    // Used to exclude idle gaps (app killed, session abandoned for hours) from the
    // recorded duration on resume, and as the staleness reference. 0 = legacy draft.
    val lastSavedAt: Long = 0L,
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
    // Stored as "mm:ss" or "ss" — empty for REPS_WEIGHT exercises. Default preserves
    // compat with drafts written before the field existed.
    val duration: String = "",
)

@Singleton
class ActiveWorkoutStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("kalos_active_workout", Context.MODE_PRIVATE)

    // ignoreUnknownKeys: tolerate new fields added in future builds.
    // coerceInputValues: tolerate null sent on non-null fields with defaults.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Bumped on every save/clear so observers (e.g. WorkoutScreen banner) re-read the draft
    // without needing a lifecycle hook or polling. Distinct from the draft content itself —
    // we just emit a version number, then map { load() } to materialize the current state.
    private val _version = MutableStateFlow(0)

    val draftFlow: Flow<WorkoutDraft?> = _version.map { load() }

    fun save(draft: WorkoutDraft) {
        prefs.edit().putString(KEY, json.encodeToString(WorkoutDraft.serializer(), draft)).apply()
        _version.update { it + 1 }
    }

    fun load(): WorkoutDraft? {
        val raw = prefs.getString(KEY, null) ?: return null
        return try {
            json.decodeFromString(WorkoutDraft.serializer(), raw)
        } catch (e: Exception) {
            // Don't silently return null — a corrupt or incompatible draft used to delete
            // the user's in-progress session without warning. Log so it surfaces in bug reports.
            Log.w(TAG, "Failed to decode active-workout draft, discarding", e)
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        _version.update { it + 1 }
    }

    companion object {
        private const val KEY = "draft"
        private const val TAG = "ActiveWorkoutStore"
        const val EXPIRY_MS = 24 * 60 * 60 * 1000L
    }
}

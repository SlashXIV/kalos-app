package com.kalos.app.core.domain.model

/**
 * Historical load reference for an exercise, surfaced as a memory aid during an
 * active workout. Computed from finished sessions only (the in-progress draft is
 * not yet persisted to the log, so it never contaminates these figures).
 *
 * @param prKg            heaviest completed set ever (personal record).
 * @param lastSessionTopKg top completed set of the most recent session, null if the
 *                         only history is the PR session itself or none.
 */
data class ExerciseReference(
    val prKg: Float,
    val lastSessionTopKg: Float?,
    // Completed sets of the most recent session (reps × weight), ordered — the in-session
    // "what did I do last time" detail line. Empty if no weighted history.
    val lastSessionSets: List<SetSummary> = emptyList(),
)

/** A single completed set, for the in-session recent-history line. */
data class SetSummary(val reps: Int, val weightKg: Float)

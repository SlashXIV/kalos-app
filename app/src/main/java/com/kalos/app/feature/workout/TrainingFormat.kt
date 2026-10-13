package com.kalos.app.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kalos.app.R
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.ui.util.formatSecsAsDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Mises en forme partagées par les écrans Sport (chiffres seuls ou textes de strings_training). */

/** Chronomètre : 75 → « 1:15 », 3 725 → « 1:02:05 ». */
fun formatClock(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Durée de séance : « 45 min », « 1 h 05 ». */
@Composable
fun trainingDuration(secs: Long): String {
    val h = (secs / 3600).toInt()
    val m = ((secs % 3600) / 60).toInt()
    return if (h > 0) stringResource(R.string.training_duration_hm, h, "%02d".format(m))
    else stringResource(R.string.training_duration_m, m)
}

/** Une série terminée : « 8 × 82,5 kg », « 1:30 », « 1:30 @ 20 kg ». */
@Composable
fun setText(set: WorkoutSet, mode: ExerciseTrackingMode): String {
    val kg = stringResource(R.string.kg)
    return when (mode) {
        ExerciseTrackingMode.REPS_WEIGHT -> when {
            set.reps > 0 && set.weightKg > 0f -> "${set.reps} × ${formatKg(set.weightKg)} $kg"
            set.reps > 0 -> stringResource(R.string.training_reps_only, set.reps)
            set.weightKg > 0f -> "${formatKg(set.weightKg)} $kg"
            else -> "—"
        }
        ExerciseTrackingMode.DURATION -> formatSecsAsDuration(set.durationSecs).ifEmpty { "—" }
        ExerciseTrackingMode.DURATION_WEIGHT -> {
            val dur = formatSecsAsDuration(set.durationSecs).ifEmpty { "—" }
            if (set.weightKg > 0f) "$dur @ ${formatKg(set.weightKg)} $kg" else dur
        }
    }
}

/** « aujourd’hui », « hier », « 3 mars », « 3 mars 2025 ». */
@Composable
fun logDateShort(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.training_today)
        today.minusDays(1) -> stringResource(R.string.training_yesterday)
        else -> d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMM" else "d MMM yyyy", Locale.FRENCH))
    }
}

/** « Lundi 3 mars 2025 » (ou aujourd’hui / hier). */
@Composable
fun logDateLong(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> d.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)).replaceFirstChar { it.uppercase() }
    }
}

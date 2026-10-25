package com.kalos.app.feature.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.LogExercise
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.GraduatedScale
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.kalos.app.core.ui.motion.CountUp
import com.kalos.app.core.ui.motion.LocalMotionLevel
import com.kalos.app.core.ui.motion.Motion
import kotlinx.coroutines.delay

/** Briques communes au récapitulatif et au détail d'une séance passée. */

/**
 * Durée / volume / séries en grands chiffres tabulaires, chaque mesure lue en une phrase.
 * [countUp] : le volume est compté jusqu'à sa valeur (moment « séance terminée »).
 */
@Composable
fun SessionStats(log: WorkoutLog, modifier: Modifier = Modifier, countUp: Boolean = false) {
    val sets = log.exercises.sumOf { le -> le.sets.count { it.isCompleted } }
    val h = (log.durationSecs / 3600).toInt()
    val m = ((log.durationSecs % 3600) / 60).toInt()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatColumn(
            label = stringResource(R.string.training_stat_duration),
            value = if (h > 0) "$h:%02d".format(m) else "$m",
            unit = stringResource(if (h > 0) R.string.training_unit_h else R.string.training_unit_min),
            spoken = trainingDuration(log.durationSecs),
            modifier = Modifier.weight(1f),
        )
        if (log.totalVolumeKg > 0f) {
            val kg = formatGroupedInt(log.totalVolumeKg.roundToInt())
            StatColumn(
                label = stringResource(R.string.training_stat_volume),
                value = kg,
                unit = stringResource(R.string.kg),
                spoken = stringResource(R.string.training_kg_spoken, kg),
                modifier = Modifier.weight(1.4f),
                countTo = if (countUp) log.totalVolumeKg.toDouble() else null,
            )
        }
        StatColumn(
            label = stringResource(R.string.training_stat_sets),
            value = "$sets",
            unit = null,
            spoken = pluralStringResource(R.plurals.training_sets_count, sets, sets),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    unit: String?,
    spoken: String,
    modifier: Modifier,
    countTo: Double? = null,
) {
    Column(modifier.clearAndSetSemantics { contentDescription = "$label, $spoken" }) {
        InstrumentLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            if (countTo != null) {
                CountUp(
                    countTo, { formatGroupedInt(it.roundToInt()) },
                    Modifier.weight(1f, fill = false), style = KalosTheme.type.metricLarge,
                )
            } else {
                FitText(value, style = KalosTheme.type.metricLarge, modifier = Modifier.weight(1f, fill = false))
            }
            if (unit != null) {
                Text(" $unit", style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Règle graduée « séries validées sur prévues » d'un exercice ; rien si aucune série n'était prévue. */
@Composable
fun ExerciseSetsScale(logExercise: LogExercise, revealDelayMs: Int? = null) {
    val planned = logExercise.sets.size
    if (planned == 0) return
    val done = logExercise.sets.count { it.isCompleted }
    // [revealDelayMs] : la règle se remplit après ce délai (les exercices l'un après l'autre).
    val level = LocalMotionLevel.current
    val reveal = remember { Animatable(if (revealDelayMs != null && level.decorative) 0f else 1f) }
    LaunchedEffect(level) {
        if (reveal.value == 1f) return@LaunchedEffect
        if (!level.decorative) return@LaunchedEffect reveal.snapTo(1f)
        delay(revealDelayMs?.toLong() ?: 0L)
        reveal.animateTo(1f, tween(Motion.ENTER_MS, easing = Motion.Enter))
    }
    val complete = done >= planned
    val sentence = pluralStringResource(R.plurals.training_sets_scale_spoken, done, done, planned) +
        if (complete) ", " + stringResource(R.string.state_goal_reached) else ""
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = sentence }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InstrumentLabel(stringResource(R.string.training_stat_sets), Modifier.weight(1f))
            Text(
                "$done / $planned",
                style = KalosTheme.type.metricSmall,
                color = if (complete) KalosTheme.nutrients.goalReached else MaterialTheme.colorScheme.onSurface,
            )
        }
        GraduatedScale(
            value = done * reveal.value,
            goal = planned.toFloat(),
            color = if (complete) KalosTheme.nutrients.goalReached else MaterialTheme.colorScheme.primary,
            ticks = 20,
        )
    }
}

/** Ligne d'une série terminée : n°, valeur tabulaire, crayon ; toute la ligne ouvre la correction. */
@Composable
fun CompletedSetLine(index: Int, set: WorkoutSet, mode: ExerciseTrackingMode, onEdit: () -> Unit) {
    val value = setText(set, mode)
    val spoken = stringResource(R.string.training_set_line_spoken, index, value)
    val action = stringResource(R.string.training_edit_set)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onEdit)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = action) { onEdit(); true }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$index", style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
        Text(value, style = KalosTheme.type.metric, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

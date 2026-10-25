package com.kalos.app.feature.workout.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.MeSeriesChart
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.feature.workout.logDateShort
import com.kalos.app.feature.workout.trainingDuration
import com.kalos.app.navigation.Screen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            ScreenHeader(
                title = stringResource(R.string.training_tab_history),
                small = true,
                modifier = Modifier.padding(horizontal = 16.dp),
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            ) {
                if (logs.isNotEmpty()) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(buildWorkoutClipboard(logs)))
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.training_copied)) }
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.training_copy_history))
                    }
                }
            }
            WorkoutHistoryContent(
                viewModel = viewModel,
                navController = navController,
                bottomPadding = padding.calculateBottomPadding() + 24.dp,
            )
        }
    }
}

@Composable
fun WorkoutHistoryTabContent(
    navController: NavController,
    bottomPadding: Dp,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        WorkoutHistoryContent(
            viewModel = viewModel,
            navController = navController,
            bottomPadding = bottomPadding,
            onCopyClick = if (logs.isNotEmpty()) ({
                clipboardManager.setText(AnnotatedString(buildWorkoutClipboard(logs)))
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.training_copied)) }
            }) else null,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding),
        )
    }
}

@Composable
private fun WorkoutHistoryContent(
    viewModel: WorkoutHistoryViewModel,
    navController: NavController,
    bottomPadding: Dp,
    onCopyClick: (() -> Unit)? = null,
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val weeklyVolumes = remember(logs) { computeWeeklyVolumes(logs) }

    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.training_empty_history),
                subtitle = stringResource(R.string.training_empty_history_hint),
                icon = Icons.Filled.History,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (onCopyClick != null) {
                item(key = "copy") {
                    // Action libellée plutôt qu'une icône seule : « copier quoi ? » a une réponse visible.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCopyClick) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.training_copy_history), style = KalosTheme.type.action)
                        }
                    }
                }
            }
            if (weeklyVolumes.size >= 2) {
                item(key = "chart") { VolumeChartCard(weeklyVolumes) }
            }
            items(logs, key = { it.id }) { log ->
                WorkoutLogCard(
                    log = log,
                    onClick = { navController.navigate(Screen.WorkoutLogDetail.route(log.id)) },
                )
            }
        }
    }
}

internal data class WeeklyVolume(val weekStart: LocalDate, val volumeKg: Float)

internal fun computeWeeklyVolumes(logs: List<WorkoutLog>): List<WeeklyVolume> {
    val byWeek = logs
        .filter { it.totalVolumeKg > 0f }
        .groupBy { LocalDate.parse(it.date).with(WeekFields.ISO.dayOfWeek(), 1) }
        .mapValues { (_, weekLogs) -> weekLogs.sumOf { it.totalVolumeKg.toDouble() }.toFloat() }
    val lastWeek = byWeek.keys.maxOrNull() ?: return emptyList()
    val firstWeek = maxOf(byWeek.keys.min(), lastWeek.minusWeeks(7))
    // Semaines consécutives : une semaine sans séance est une barre à zéro, pas un trou effacé.
    return generateSequence(firstWeek) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(lastWeek) }
        .map { WeeklyVolume(it, byWeek[it] ?: 0f) }
        .toList()
}

@Composable
private fun VolumeChartCard(weeks: List<WeeklyVolume>) {
    val title = stringResource(R.string.training_weekly_volume)
    val readings = weeks.map {
        stringResource(R.string.training_week_volume_spoken, it.weekStart.format(WEEK_FORMAT), formatGroupedInt(it.volumeKg.roundToInt()))
    }
    KalosCard {
        SectionLabel(title)
        MeSeriesChart(
            values = weeks.map { it.volumeKg },
            reading = { readings[it] },
            summary = title,
            color = MaterialTheme.colorScheme.primary,
            ticks = listOf(weeks.maxOf { it.volumeKg }),
        )
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(weeks.first().weekStart.format(WEEK_FORMAT), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(weeks.last().weekStart.format(WEEK_FORMAT), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            pluralStringResource(
                R.plurals.training_weekly_total, weeks.size, weeks.size,
                formatGroupedInt(weeks.sumOf { it.volumeKg.toDouble() }.roundToInt()),
            ),
            style = KalosTheme.type.metricSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val WEEK_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

@Composable
private fun WorkoutLogCard(log: WorkoutLog, onClick: () -> Unit) {
    val completed = log.exercises.sumOf { le -> le.sets.count { it.isCompleted } }
    val parts = buildList {
        add(trainingDuration(log.durationSecs))
        if (log.exercises.isNotEmpty()) {
            add(pluralStringResource(R.plurals.training_exercise_count_only, log.exercises.size, log.exercises.size))
        }
        if (completed > 0) add(pluralStringResource(R.plurals.training_sets_count, completed, completed))
        if (log.totalVolumeKg > 0f) {
            add(stringResource(R.string.training_kg_value, formatGroupedInt(log.totalVolumeKg.roundToInt())))
        }
        log.energyKcal?.let { add(stringResource(R.string.training_activity_energy_short, formatGroupedInt(it))) }
    }
    KalosCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                log.templateName.ifBlank { stringResource(R.string.training_free_workout) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(logDateShort(log.date), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(parts.joinToString(" · "), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDurationPlain(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h%02d".format(m) else "${m}min"
}

// texte-technique : historique copié dans le presse-papiers, au format texte brut.
private fun buildWorkoutClipboard(logs: List<WorkoutLog>): String {
    val recent = logs.take(10)
    val headerDate = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
    val sessionFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

    val sb = StringBuilder()
    sb.appendLine("Kalos — ${recent.size} dernières séances ($headerDate)")

    for (log in recent) {
        sb.appendLine()
        val dateLabel = LocalDate.parse(log.date)
            .format(sessionFmt)
            .replaceFirstChar { it.uppercase() }
        val dur = formatDurationPlain(log.durationSecs)
        val vol = if (log.totalVolumeKg > 0f) " — %.0f kg".format(log.totalVolumeKg) else ""
        sb.appendLine("$dateLabel — ${log.templateName.ifBlank { "Séance libre" }} — $dur$vol")

        for (le in log.exercises) {
            val done = le.sets.filter { it.isCompleted }
            if (done.isEmpty()) continue
            val best = done.maxByOrNull { it.weightKg } ?: continue
            val wStr = if (best.weightKg == best.weightKg.toLong().toFloat())
                "${best.weightKg.toLong()} kg" else "%.1f kg".format(best.weightKg)
            sb.appendLine("  ${le.exercise.name.take(26).padEnd(26)}   ${done.size} × ${best.reps} @ $wStr")
        }
    }

    return sb.toString().trimEnd()
}

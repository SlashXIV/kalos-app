package com.kalos.app.feature.workout.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.kalos.app.core.ui.util.formatGroupedInt
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(buildWorkoutClipboard(logs)))
                            scope.launch { snackbarHostState.showSnackbar("Copié") }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copier l'historique")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        WorkoutHistoryContent(
            viewModel = viewModel,
            navController = navController,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun WorkoutHistoryTabContent(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        WorkoutHistoryContent(
            viewModel = viewModel,
            navController = navController,
            onCopyClick = if (logs.isNotEmpty()) ({
                clipboardManager.setText(AnnotatedString(buildWorkoutClipboard(logs)))
                scope.launch { snackbarHostState.showSnackbar("Copié") }
            }) else null,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WorkoutHistoryContent(
    viewModel: WorkoutHistoryViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onCopyClick: (() -> Unit)? = null,
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val weeklyVolumes = remember(logs) { computeWeeklyVolumes(logs) }

    if (logs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Aucune séance enregistrée",
                subtitle = "Terminez votre première séance pour voir votre historique",
                icon = Icons.Filled.History,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            if (onCopyClick != null) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onCopyClick) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copier l'historique",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (weeklyVolumes.size >= 2) {
                item { VolumeChartCard(weeklyVolumes) }
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

private data class WeeklyVolume(val weekStart: LocalDate, val volumeKg: Float)

private fun computeWeeklyVolumes(logs: List<WorkoutLog>): List<WeeklyVolume> {
    val isoWeek = WeekFields.ISO
    return logs
        .filter { it.totalVolumeKg > 0f }
        .groupBy {
            val date = LocalDate.parse(it.date)
            date.with(isoWeek.dayOfWeek(), 1)
        }
        .map { (weekStart, weekLogs) ->
            WeeklyVolume(weekStart, weekLogs.sumOf { it.totalVolumeKg.toDouble() }.toFloat())
        }
        .sortedBy { it.weekStart }
        .takeLast(8)
}

@Composable
private fun VolumeChartCard(weeks: List<WeeklyVolume>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

    val maxVolume = weeks.maxOf { it.volumeKg }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Volume hebdomadaire", style = MaterialTheme.typography.titleSmall)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val bottomPadding = 20f
                val leftPadding = 8f
                val chartWidth = size.width - leftPadding
                val chartHeight = size.height - bottomPadding
                val barCount = weeks.size
                val slotWidth = chartWidth / barCount
                val barWidth = slotWidth * 0.55f

                // Horizontal grid (3 lines)
                repeat(4) { i ->
                    val y = chartHeight * (1f - i / 3f)
                    drawLine(gridColor, Offset(leftPadding, y), Offset(size.width, y), strokeWidth = 1f)
                }

                weeks.forEachIndexed { i, week ->
                    val barHeight = if (maxVolume > 0f) (week.volumeKg / maxVolume) * chartHeight else 0f
                    val x = leftPadding + i * slotWidth + (slotWidth - barWidth) / 2f
                    val y = chartHeight - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f),
                    )

                    // X label (first and last, plus middle if 8 bars)
                    if (i == 0 || i == barCount - 1 || (barCount >= 6 && i == barCount / 2)) {
                        val label = textMeasurer.measure(week.weekStart.format(dateFmt), style = labelStyle)
                        val labelX = (x + barWidth / 2f - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
                        drawText(label, topLeft = Offset(labelX, chartHeight + 4f))
                    }
                }
            }
            Text(
                "Total sur ${weeks.size} semaines : ${formatGroupedInt(weeks.sumOf { it.volumeKg.toDouble() })} kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutLogCard(log: WorkoutLog, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    log.templateName.ifBlank { "Séance libre" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatLogDateShort(log.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LogStat(Icons.Filled.Timer, formatDuration(log.durationSecs))
                LogStat(Icons.Filled.FitnessCenter, "${log.exercises.size} exercice${if (log.exercises.size > 1) "s" else ""}")
                val completed = log.exercises.sumOf { le -> le.sets.count { it.isCompleted } }
                if (completed > 0) LogStat(Icons.Filled.CheckCircle, "$completed série${if (completed > 1) "s" else ""}")
                if (log.totalVolumeKg > 0f) LogStat(Icons.Filled.MonitorWeight, "%.0f kg".format(log.totalVolumeKg))
            }
        }
    }
}

@Composable
private fun LogStat(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h%02d".format(m) else "${m}min"
}

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
        val dur = formatDuration(log.durationSecs)
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

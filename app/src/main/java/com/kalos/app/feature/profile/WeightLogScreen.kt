package com.kalos.app.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class WeightLogUiState(
    val entries: List<Pair<String, Float>> = emptyList(),
    val isSaving: Boolean = false,
)

@HiltViewModel
class WeightLogViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)

    val uiState: StateFlow<WeightLogUiState> = combine(
        workoutRepository.getBodyWeightHistory(),
        _isSaving,
    ) { entries, isSaving ->
        WeightLogUiState(entries = entries, isSaving = isSaving)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightLogUiState(),
    )

    fun logWeight(weightKg: Float) {
        viewModelScope.launch {
            _isSaving.value = true
            workoutRepository.logBodyWeight(LocalDate.now().toString(), weightKg)
            _isSaving.value = false
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightLogScreen(
    navController: NavController,
    viewModel: WeightLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogDialog by remember { mutableStateOf(false) }

    val lastEntry = state.entries.firstOrNull()
    val previousEntry = state.entries.getOrNull(1)
    val delta = if (lastEntry != null && previousEntry != null) {
        lastEntry.second - previousEntry.second
    } else null

    val today = LocalDate.now()
    val windowStart = today.minusDays(29)
    val chartEntries = remember(state.entries) {
        state.entries
            .filter { !LocalDate.parse(it.first).isBefore(windowStart) }
            .sortedBy { it.first }
    }

    if (showLogDialog) {
        WeightInputDialog(
            currentWeight = lastEntry?.second,
            onConfirm = { kg ->
                viewModel.logWeight(kg)
                showLogDialog = false
            },
            onDismiss = { showLogDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suivi du poids") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Summary card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Dernier enregistrement", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (lastEntry != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${"%.1f".format(lastEntry.second)} kg",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatDate(lastEntry.first),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (delta != null) {
                                    val sign = if (delta >= 0f) "+" else ""
                                    Text(
                                        "$sign${"%.1f".format(delta)} kg",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Aucune entrée enregistrée",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Chart — shown only if at least 2 data points in the 30-day window
            if (chartEntries.size >= 2) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Évolution — 30 derniers jours",
                            style = MaterialTheme.typography.titleSmall)
                        WeightLineChart(
                            entries = chartEntries,
                            windowStart = windowStart,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant,
                            onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                formatDateShort(windowStart.toString()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Auj.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Log button
            Button(
                onClick = { showLogDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "Enregistrement…" else "Enregistrer le poids")
            }

            // Recent entries list
            if (state.entries.isNotEmpty()) {
                Text("Historique récent",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        state.entries.take(10).forEachIndexed { index, entry ->
                            val prev = state.entries.getOrNull(index + 1)
                            val d = if (prev != null) entry.second - prev.second else null
                            WeightEntryRow(date = entry.first, weightKg = entry.second, delta = d)
                            if (index < minOf(state.entries.lastIndex, 9)) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Chart ───────────────────────────────────────────────────────────────────

@Composable
private fun WeightLineChart(
    entries: List<Pair<String, Float>>,
    windowStart: LocalDate,
    primaryColor: Color,
    surfaceVariantColor: Color,
    onSurfaceVariantColor: Color,
    modifier: Modifier = Modifier,
) {
    val weights = entries.map { it.second }
    val minW = (weights.min() - 1f).let { kotlin.math.floor(it.toDouble()).toFloat() }
    val maxW = (weights.max() + 1f).let { kotlin.math.ceil(it.toDouble()).toFloat() }
    val range = (maxW - minW).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padV = 8.dp.toPx()
        val chartH = h - padV * 2

        // Grid line at midpoint
        val midY = padV + chartH * 0.5f
        drawLine(
            color = surfaceVariantColor,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx(),
        )

        // Compute point positions
        val points = entries.map { (dateStr, kg) ->
            val dayOffset = ChronoUnit.DAYS.between(windowStart, LocalDate.parse(dateStr)).toFloat()
            val x = (dayOffset / 29f).coerceIn(0f, 1f) * w
            val y = padV + chartH * (1f - (kg - minW) / range)
            Offset(x, y)
        }

        // Line
        if (points.size >= 2) {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Dots
        points.forEach { pt ->
            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = pt)
            drawCircle(
                color = onSurfaceVariantColor.copy(alpha = 0.1f),
                radius = 4.dp.toPx(),
                center = pt,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

// ─── Sub-composables ─────────────────────────────────────────────────────────

@Composable
private fun WeightEntryRow(date: String, weightKg: Float, delta: Float?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatDate(date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (delta != null && abs(delta) >= 0.05f) {
                val sign = if (delta >= 0f) "+" else ""
                Text(
                    "$sign${"%.1f".format(delta)} kg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${"%.1f".format(weightKg)} kg",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WeightInputDialog(
    currentWeight: Float?,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(currentWeight?.let { "%.1f".format(it) } ?: "") }
    val parsed = input.replace(',', '.').toFloatOrNull()
    val isValid = parsed != null && parsed in 30f..300f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer le poids") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Poids (kg)") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = input.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it) } },
                enabled = isValid,
            ) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatDate(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
    }
}

private fun formatDateShort(dateStr: String): String =
    LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))

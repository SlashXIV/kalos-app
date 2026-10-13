package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.MeChartStyle
import com.kalos.app.core.ui.component.MeSeriesChart
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class WeightLogUiState(
    val entries: List<Pair<String, Float>> = emptyList(),
    val isSaving: Boolean = false,
) {
    val hasTodayEntry: Boolean
        get() = entries.firstOrNull()?.first == LocalDate.now().toString()
}

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

@Composable
fun WeightLogScreen(
    navController: NavController,
    viewModel: WeightLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogDialog by remember { mutableStateOf(false) }

    val lastEntry = state.entries.firstOrNull()
    val previousEntry = state.entries.getOrNull(1)
    val delta = if (lastEntry != null && previousEntry != null) lastEntry.second - previousEntry.second else null

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
            isUpdate = state.hasTodayEntry,
            onConfirm = { kg ->
                viewModel.logWeight(kg)
                showLogDialog = false
            },
            onDismiss = { showLogDialog = false },
        )
    }

    val kgUnit = stringResource(R.string.kg)
    val kgSpoken = stringResource(R.string.me_unit_kg_spoken)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.me_weight_title),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )

            KalosCard {
                InstrumentLabel(stringResource(R.string.me_weight_last))
                if (lastEntry != null) {
                    val date = formatDate(lastEntry.first)
                    val sentence = stringResource(R.string.me_value_spoken, date, kg1(lastEntry.second), kgSpoken) +
                        if (delta != null) ", " + stringResource(R.string.me_weight_delta_spoken, signed(delta)) else ""
                    Row(
                        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = sentence },
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FitText(kg1(lastEntry.second), style = KalosTheme.type.metricHuge)
                            Text(kgUnit, style = KalosTheme.type.metric, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (delta != null) {
                                Text("${signed(delta)} $kgUnit", style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.me_weight_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Graphique : au moins deux pesées sur les 30 derniers jours.
            if (chartEntries.size >= 2) {
                KalosCard {
                    SectionLabel(stringResource(R.string.me_weight_chart))
                    val weights = chartEntries.map { it.second }
                    val readings = chartEntries.map { (d, kg) ->
                        stringResource(R.string.me_value_spoken, shortDate(d), kg1(kg), kgSpoken)
                    }
                    MeSeriesChart(
                        values = weights,
                        reading = { readings[it] },
                        summary = pluralStringResource(R.plurals.me_weight_chart_summary, chartEntries.size, chartEntries.size),
                        style = MeChartStyle.Line,
                        color = MaterialTheme.colorScheme.primary,
                        xFractions = chartEntries.map { (d, _) ->
                            (ChronoUnit.DAYS.between(windowStart, LocalDate.parse(d)) / 29f).coerceIn(0f, 1f)
                        },
                        ticks = listOf(floor(weights.min()), ceil(weights.max())).distinct(),
                        tickLabel = { "%.0f".format(it) },
                        endLabel = "${kg1(weights.last())} $kgUnit",
                    )
                    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(shortDate(windowStart.toString()), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.me_today_short), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            KalosButton(
                text = stringResource(
                    when {
                        state.isSaving -> R.string.me_weight_saving
                        state.hasTodayEntry -> R.string.me_weight_update
                        else -> R.string.me_weight_log
                    },
                ),
                onClick = { showLogDialog = true },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.entries.isNotEmpty()) {
                SectionLabel(stringResource(R.string.me_weight_history), Modifier.padding(top = 8.dp))
                KalosCard {
                    state.entries.take(10).forEachIndexed { index, entry ->
                        val prev = state.entries.getOrNull(index + 1)
                        WeightEntryRow(date = entry.first, weightKg = entry.second, delta = prev?.let { entry.second - it.second })
                        if (index < minOf(state.entries.lastIndex, 9)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

// ─── Sub-composables ─────────────────────────────────────────────────────────

@Composable
private fun WeightEntryRow(date: String, weightKg: Float, delta: Float?) {
    val kg = stringResource(R.string.kg)
    val label = formatDate(date)
    val shownDelta = delta?.takeIf { abs(it) >= 0.05f }
    val sentence = stringResource(R.string.me_value_spoken, label, kg1(weightKg), stringResource(R.string.me_unit_kg_spoken)) +
        if (shownDelta != null) ", " + stringResource(R.string.me_weight_delta_spoken, signed(shownDelta)) else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clearAndSetSemantics { contentDescription = sentence },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (shownDelta != null) {
            Text("${signed(shownDelta)} $kg", style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${kg1(weightKg)} $kg", style = KalosTheme.type.metric)
    }
}

@Composable
private fun WeightInputDialog(
    currentWeight: Float?,
    isUpdate: Boolean,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(currentWeight?.let { kg1(it) } ?: "") }
    val parsed = input.replace(',', '.').toFloatOrNull()
    val isValid = parsed != null && parsed in 30f..300f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isUpdate) R.string.me_weight_update else R.string.me_weight_log)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KalosNumberField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.me_weight_field)) },
                    suffix = { Text(stringResource(R.string.kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            TextButton(onClick = { parsed?.let { onConfirm(it) } }, enabled = isValid) {
                Text(stringResource(R.string.action_validate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun kg1(kg: Float): String = "%.1f".format(Locale.FRENCH, kg)

private fun signed(v: Float): String = (if (v >= 0f) "+" else "") + kg1(v)

@Composable
private fun formatDate(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
    }
}

private fun shortDate(dateStr: String): String =
    LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)).replaceFirstChar { it.uppercase() }


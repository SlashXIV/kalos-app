package com.kalos.app.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.ui.util.dayLabel
import androidx.hilt.navigation.compose.hiltViewModel
import com.kalos.app.core.domain.model.ActivityType
import com.kalos.app.core.ui.util.formatGroupedInt
import androidx.annotation.StringRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalos.app.core.ui.component.KalosNumberField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogActivityDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LogActivityViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<ActivityType?>(null) }
    val weightKg by viewModel.weight.collectAsStateWithLifecycle()
    val labels = ActivityType.entries.associateWith { stringResource(activityLabelRes(it)) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val h = hours.toIntOrNull() ?: 0
    val m = minutes.toIntOrNull() ?: 0
    val durationSecs = (h * 3600 + m * 60).toLong()
    val canSave = name.isNotBlank() && durationSecs > 0

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_validate)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.training_log_activity)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { v ->
                        name = v
                        type = labels.entries.firstOrNull { it.value.equals(v.trim(), ignoreCase = true) }?.key
                    },
                    label = { Text(stringResource(R.string.training_activity)) },
                    placeholder = { Text(stringResource(R.string.training_activity_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { name = label; type = t },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KalosNumberField(
                        value = hours,
                        onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.training_hours)) },
                        modifier = Modifier.weight(1f),
                    )
                    KalosNumberField(
                        value = minutes,
                        onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.training_minutes)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                viewModel.estimate(type, weightKg, durationSecs)?.let { kcal ->
                    Text(
                        stringResource(R.string.training_activity_energy, formatGroupedInt(kcal)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                com.kalos.app.core.ui.component.KalosButton(
                    stringResource(R.string.training_date_value, formatActivityDate(date)),
                    onClick = { showDatePicker = true },
                    ghost = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.training_note_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    viewModel.logActivity(name, type, durationSecs, date.toString(), note)
                    onSaved()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@StringRes
fun activityLabelRes(type: ActivityType): Int = when (type) {
    ActivityType.CLIMBING -> R.string.activity_climbing
    ActivityType.HIKING -> R.string.activity_hiking
    ActivityType.SWIMMING -> R.string.activity_swimming
    ActivityType.RUNNING -> R.string.activity_running
    ActivityType.CYCLING -> R.string.activity_cycling
    ActivityType.FOOTBALL -> R.string.activity_football
    ActivityType.YOGA -> R.string.activity_yoga
    ActivityType.BOXING -> R.string.activity_boxing
    ActivityType.TENNIS -> R.string.activity_tennis
    ActivityType.SKIING -> R.string.activity_skiing
}

@Composable
private fun formatActivityDate(date: LocalDate): String = dayLabel(date, "d MMM yyyy")

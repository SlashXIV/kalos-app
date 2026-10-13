package com.kalos.app.feature.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.component.KalosNumberField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogActivityViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    /** Records a free activity (e.g. climbing 2h) as a lightweight session with no exercises. */
    fun logActivity(name: String, durationSecs: Long, date: String, note: String) {
        val log = WorkoutLog(
            templateName = name.trim(),
            date = date,
            startedAt = System.currentTimeMillis(),
            notes = note.trim(),
            exercises = emptyList(),
        )
        viewModelScope.launch { workoutRepository.completeWorkout(log, durationSecs) }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogActivityDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LogActivityViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
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
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.training_activity)) },
                    placeholder = { Text(stringResource(R.string.training_activity_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    stringArrayResource(R.array.training_activity_suggestions).forEach { s ->
                        SuggestionChip(
                            onClick = { name = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) },
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
                    viewModel.logActivity(name, durationSecs, date.toString(), note)
                    onSaved()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun formatActivityDate(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
    }
}

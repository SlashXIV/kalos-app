package com.kalos.app.feature.workout.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.getValue

@Composable
fun ProgramDetailScreen(
    navController: NavController,
    programId: Long,
    viewModel: ProgramDetailViewModel = hiltViewModel(),
) {
    val program by viewModel.program.collectAsStateWithLifecycle()
    val notif by viewModel.notif.collectAsStateWithLifecycle()
    LaunchedEffect(programId) { viewModel.load(programId) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        val p = program
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = p?.name ?: stringResource(R.string.training_program),
                    eyebrow = stringResource(R.string.training_program),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    },
                ) {
                    if (p?.isActive == false) {
                        TextButton(onClick = viewModel::activate) { Text(stringResource(R.string.training_activate)) }
                    }
                }
            }
            if (p == null) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                return@LazyColumn
            }

            item(key = "summary") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (p.isActive) {
                        InstrumentLabel(stringResource(R.string.training_program_active), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        pluralStringResource(R.plurals.training_program_rhythm, p.durationWeeks, p.durationWeeks, p.daysPerWeek),
                        style = KalosTheme.type.metric,
                    )
                    if (p.description.isNotBlank()) {
                        Text(p.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (p.workouts.isNotEmpty()) {
                item(key = "weekLabel") { SectionLabel(stringResource(R.string.training_program_week)) }
                item(key = "week") {
                    KalosCard {
                        p.workouts.sortedWith(compareBy({ it.weekNumber }, { it.dayOfWeek })).forEach { ProgramWorkoutRow(it) }
                    }
                }
            } else {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.training_program_no_schedule),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "notif") {
                NotifSettingsCard(
                    notif = notif,
                    onEnabled = viewModel::setNotifEnabled,
                    onDayOf = viewModel::setNotifDayOf,
                    onDayBefore = viewModel::setNotifDayBefore,
                )
            }
        }
    }
}

@Composable
private fun NotifSettingsCard(
    notif: ProgramNotifState,
    onEnabled: (Boolean) -> Unit,
    onDayOf: (Boolean) -> Unit,
    onDayBefore: (Boolean) -> Unit,
) {
    KalosCard {
        SectionLabel(stringResource(R.string.training_reminders))
        NotifToggleRow(stringResource(R.string.training_reminders_enable), null, notif.enabled, onEnabled)
        if (notif.enabled) {
            NotifToggleRow(
                stringResource(R.string.training_reminder_day_of),
                stringResource(R.string.training_reminder_day_of_hint),
                notif.dayOf,
                onDayOf,
            )
            NotifToggleRow(
                stringResource(R.string.training_reminder_day_before),
                stringResource(R.string.training_reminder_day_before_hint),
                notif.dayBefore,
                onDayBefore,
            )
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    sublabel: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (sublabel != null) {
                Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ProgramWorkoutRow(pw: ProgramWorkout) {
    val day = if (pw.dayOfWeek in 1..7) DayOfWeek.of(pw.dayOfWeek).getDisplayName(TextStyle.FULL, Locale.FRENCH) else ""
    val name = pw.template?.name ?: stringResource(R.string.training_free_workout)
    val count = pw.template?.exerciseCount ?: 0
    val week = if (pw.weekNumber > 1) stringResource(R.string.training_week_number, pw.weekNumber) else null
    val exercises = if (count > 0) pluralStringResource(R.plurals.training_exercise_short, count, count) else null
    val spoken = listOfNotNull(day, name, week, exercises).filter { it.isNotEmpty() }.joinToString(", ")
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstrumentLabel(day.take(3), Modifier.width(40.dp), color = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            if (week != null) {
                Text(week, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (exercises != null) {
            Text(exercises, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

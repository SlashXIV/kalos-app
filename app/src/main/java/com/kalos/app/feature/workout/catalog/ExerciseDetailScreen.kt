package com.kalos.app.feature.workout.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.MeChartStyle
import com.kalos.app.core.ui.component.MeSeriesChart
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.feature.workout.logDateShort
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.runtime.getValue

@Composable
fun ExerciseDetailScreen(
    navController: NavController,
    exerciseId: Long,
    fromBuilder: Boolean = false,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val exercise = state.exercise
    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (fromBuilder) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KalosButton(
                        stringResource(R.string.training_add_to_workout),
                        onClick = {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("added_exercise_id", exerciseId)
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader(
                title = exercise?.name ?: stringResource(R.string.training_exercise),
                eyebrow = exercise?.let { listOf(it.primaryMuscle, it.type.label, it.level.label).filter { s -> s.isNotBlank() }.joinToString(" · ") },
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
            val ex = exercise ?: return@Column
            if (ex.equipment != "Aucun") {
                Text(stringResource(R.string.training_equipment, ex.equipment), style = MaterialTheme.typography.bodyMedium)
            }
            if (ex.secondaryMuscles.isNotEmpty()) {
                Text(
                    stringResource(R.string.training_secondary_muscles, ex.secondaryMuscles.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ex.description.isNotEmpty()) {
                KalosCard {
                    SectionLabel(stringResource(R.string.training_description))
                    Text(ex.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (ex.instructions.isNotEmpty()) {
                KalosCard {
                    SectionLabel(stringResource(R.string.training_instructions))
                    Text(ex.instructions, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.progression.size >= 2 || state.pr != null) {
                ExerciseProgressionCard(progression = state.progression, pr = state.pr)
            }
        }
    }
}

@Composable
private fun ExerciseProgressionCard(
    progression: List<Pair<String, Float>>,
    pr: Float?,
) {
    KalosCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.training_progression), Modifier.weight(1f))
            if (pr != null) {
                Text(
                    stringResource(R.string.training_ref_pr, formatKg(pr)),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (progression.size >= 2) {
            val dates = progression.map { LocalDate.parse(it.first) }
            val weights = progression.map { it.second }
            val spanDays = ChronoUnit.DAYS.between(dates.first(), dates.last()).coerceAtLeast(1L).toFloat()
            val kgSpoken = stringResource(R.string.me_unit_kg_spoken)
            val readings = progression.map { (d, kg) ->
                stringResource(R.string.me_value_spoken, logDateShort(d), formatKg(kg), kgSpoken)
            }
            MeSeriesChart(
                values = weights,
                reading = { readings[it] },
                summary = pluralStringResource(R.plurals.training_progression_summary, progression.size, progression.size),
                style = MeChartStyle.Line,
                color = MaterialTheme.colorScheme.primary,
                xFractions = dates.map { ChronoUnit.DAYS.between(dates.first(), it) / spanDays },
                ticks = listOf(weights.min(), weights.max()).distinct(),
                tickLabel = ::formatKg,
                endLabel = stringResource(R.string.training_kg_value, formatKg(weights.last())),
            )
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(logDateShort(progression.first().first), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(logDateShort(progression.last().first), style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (progression.size == 1) {
            Text(
                stringResource(R.string.training_progression_single),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

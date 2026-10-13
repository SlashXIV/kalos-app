package com.kalos.app.feature.workout.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.repository.ExerciseRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val progression: List<Pair<String, Float>> = emptyList(),
    val pr: Float? = null,
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state

    fun load(id: Long) {
        viewModelScope.launch {
            val exercise = repo.getById(id)
            val progression = workoutRepo.getExerciseProgression(id)
            val pr = workoutRepo.getMaxWeight(id)
            _state.value = ExerciseDetailUiState(exercise = exercise, progression = progression, pr = pr)
        }
    }
}

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
            ProgressionChart(
                data = progression,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
        } else if (progression.size == 1) {
            Text(
                stringResource(R.string.training_progression_single),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Restyle minimal (thème, chiffres tabulaires) : le graphique sera refait dans un autre lot. */
@Composable
private fun ProgressionChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()

    val labelStyle = KalosTheme.type.metricSmall.copy(fontSize = 9.sp, color = labelColor)
    val bottomPaddingPx = 24f
    val leftPaddingPx = 8f

    val dates = data.map { LocalDate.parse(it.first) }
    val weights = data.map { it.second }
    val minDate = dates.first()
    val maxDate = dates.last()
    val totalDays = maxOf(1L, java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate))
    val minWeight = weights.min()
    val maxWeight = weights.max()
    val weightRange = maxOf(1f, maxWeight - minWeight)

    Canvas(modifier = modifier) {
        val chartWidth = size.width - leftPaddingPx
        val chartHeight = size.height - bottomPaddingPx

        // Horizontal grid lines (3 levels)
        val gridSteps = 3
        repeat(gridSteps + 1) { i ->
            val y = chartHeight * (1f - i.toFloat() / gridSteps)
            drawLine(gridColor, Offset(leftPaddingPx, y), Offset(size.width, y), strokeWidth = 1f)
            val w = minWeight + weightRange * i / gridSteps
            val label = textMeasurer.measure(formatKg(w), style = labelStyle)
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
        }

        // Points
        val points = data.mapIndexed { index, (_, weight) ->
            val date = dates[index]
            val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(minDate, date)
            val x = leftPaddingPx + (dayOffset.toFloat() / totalDays) * chartWidth
            val y = chartHeight * (1f - (weight - minWeight) / weightRange)
            Offset(x, y)
        }

        // Line
        val path = Path()
        points.forEachIndexed { i, pt ->
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        // Dots
        points.forEach { pt -> drawCircle(lineColor, radius = 3.dp.toPx(), center = pt) }

        // X-axis date labels (first and last)
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        val firstLabel = textMeasurer.measure(minDate.format(fmt), style = labelStyle)
        val lastLabel = textMeasurer.measure(maxDate.format(fmt), style = labelStyle)
        drawText(firstLabel, topLeft = Offset(leftPaddingPx, chartHeight + 4f))
        drawText(lastLabel, topLeft = Offset(size.width - lastLabel.size.width, chartHeight + 4f))
    }
}

package com.kalos.app.feature.workout.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.repository.ExerciseRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercice") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            if (fromBuilder) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("added_exercise_id", exerciseId)
                        navController.popBackStack()
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Ajouter à la séance") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    ) { padding ->
        exercise?.let { ex ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
                    .let { if (fromBuilder) it.padding(bottom = 80.dp) else it },
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(ex.primaryMuscle) })
                    AssistChip(onClick = {}, label = { Text(ex.type.label) })
                    AssistChip(onClick = {}, label = { Text(ex.level.label) })
                }
                if (ex.equipment != "Aucun") {
                    Text("Matériel: ${ex.equipment}", style = MaterialTheme.typography.bodyMedium)
                }
                if (ex.secondaryMuscles.isNotEmpty()) {
                    Text(
                        "Muscles secondaires: ${ex.secondaryMuscles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ex.description.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Description", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(ex.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (ex.instructions.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Instructions", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(ex.instructions, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (state.progression.size >= 2 || state.pr != null) {
                    ExerciseProgressionCard(
                        progression = state.progression,
                        pr = state.pr,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressionCard(
    progression: List<Pair<String, Float>>,
    pr: Float?,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Progression", style = MaterialTheme.typography.titleSmall)
                if (pr != null) {
                    Text(
                        text = "PR : ${formatWeight(pr)} kg",
                        style = MaterialTheme.typography.labelLarge,
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
                    "1 séance enregistrée — le graphe s'affichera à partir de 2 séances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressionChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = rememberTextMeasurer()

    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
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
            val label = textMeasurer.measure("${formatWeight(w)}", style = labelStyle)
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
        }

        // Points
        val points = data.mapIndexed { index, (dateStr, weight) ->
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
        drawPath(path, color = lineColor, style = Stroke(width = 2.5f))

        // Dots
        points.forEach { pt -> drawCircle(dotColor, radius = 4f, center = pt) }

        // X-axis date labels (first and last)
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        val firstLabel = textMeasurer.measure(minDate.format(fmt), style = labelStyle)
        val lastLabel = textMeasurer.measure(maxDate.format(fmt), style = labelStyle)
        drawText(firstLabel, topLeft = Offset(leftPaddingPx, chartHeight + 4f))
        drawText(lastLabel, topLeft = Offset(size.width - lastLabel.size.width, chartHeight + 4f))
    }
}

private fun formatWeight(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString()
    else "%.1f".format(w)

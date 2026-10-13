package com.kalos.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.Dot
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.MeasureRow
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.navigation.Screen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Au-delà de 105 % de l'objectif, le jour est « au-dessus » (même seuil que le cadran du jour). */
private const val OVER_LIMIT = 1.05f

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayDetail by viewModel.dayDetail.collectAsStateWithLifecycle()
    val insights by viewModel.insightsState.collectAsStateWithLifecycle()
    val monthKcal by viewModel.monthKcal.collectAsStateWithLifecycle()

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
            val monthTitle = remember(state.month) {
                state.month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
                    .replaceFirstChar { it.uppercase() } + " " + state.month.year
            }
            ScreenHeader(title = monthTitle, eyebrow = stringResource(R.string.nav_calendar)) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.me_cal_month_prev))
                }
                if (state.month != YearMonth.now()) {
                    IconButton(onClick = viewModel::today) {
                        Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.me_cal_month_current))
                    }
                }
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.me_cal_month_next))
                }
            }

            KalosCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                // Initiales des jours : décor, chaque case dit déjà son jour à TalkBack.
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                    DayOfWeek.entries.forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            InstrumentLabel(day.getDisplayName(TextStyle.NARROW, Locale.FRENCH))
                        }
                    }
                }
                CalendarGrid(
                    state = state,
                    monthKcal = monthKcal,
                    kcalGoal = insights.kcalGoal,
                    selectedDate = selectedDate,
                    onDateClick = viewModel::selectDate,
                )
            }

            CalendarLegend()

            val selected = selectedDate
            if (selected != null) {
                val planned = run {
                    val dow = LocalDate.parse(selected).dayOfWeek.value
                    if (selected in state.plannedWorkoutDates)
                        state.activeProgram?.workouts?.firstOrNull { it.dayOfWeek == dow }
                    else null
                }
                DayDetailCard(
                    selectedDate = selected,
                    detail = dayDetail,
                    kcalGoal = insights.kcalGoal,
                    plannedWorkoutName = planned?.template?.name,
                    activeProgramName = if (planned != null) state.activeProgram?.name else null,
                    onNavigateToNutrition = { navController.navigate(Screen.Nutrition.route) },
                    isToday = selected == LocalDate.now().toString(),
                )
            }

            InsightsSection(insights = insights)
        }
    }
}

// ─── Grille du mois ───────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    monthKcal: Map<String, Float>,
    kcalGoal: Float,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
) {
    val month = state.month
    val today = LocalDate.now()
    val cells = remember(month) {
        buildList<Int?> {
            repeat(month.atDay(1).dayOfWeek.value - 1) { add(null) }
            for (d in 1..month.lengthOfMonth()) add(d)
            while (size % 7 != 0) add(null)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f)) {
                        if (day != null) {
                            val date = month.atDay(day)
                            val dateStr = date.toString()
                            DayCell(
                                date = date,
                                kcal = monthKcal[dateStr] ?: 0f,
                                kcalGoal = kcalGoal,
                                isToday = date == today,
                                isSelected = dateStr == selectedDate,
                                hasWorkout = dateStr in state.workoutDates,
                                hasPlanned = dateStr in state.plannedWorkoutDates,
                                isFuture = date.isAfter(today),
                                onClick = { onDateClick(dateStr) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    kcal: Float,
    kcalGoal: Float,
    isToday: Boolean,
    isSelected: Boolean,
    hasWorkout: Boolean,
    hasPlanned: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val over = kcalGoal > 0f && kcal > kcalGoal * OVER_LIMIT
    val ink = if (isSelected) colors.surface else colors.onSurface
    val gaugeFill = if (over) KalosTheme.nutrients.overTarget else ink

    val sentence = buildList {
        add(if (isToday) stringResource(R.string.me_cal_today_day, longDate(date)) else longDate(date))
        val k = kcal.roundToInt()
        if (k > 0) {
            add(pluralStringResource(R.plurals.me_kcal_spoken, k, formatGroupedInt(k)))
            if (over) add(stringResource(R.string.state_over_target))
        }
        if (hasWorkout) add(stringResource(R.string.me_cal_workout_done))
        else if (hasPlanned) add(stringResource(R.string.me_cal_workout_planned))
    }.joinToString(", ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(KalosShape.control)
            .then(
                when {
                    isSelected -> Modifier.background(colors.onSurface)
                    isToday -> Modifier.border(1.5.dp, colors.onSurface, KalosShape.control)
                    else -> Modifier
                },
            )
            .clickable(enabled = !isFuture, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = sentence
                role = Role.Button
                this.selected = isSelected
                if (isFuture) disabled() else onClick { onClick(); true }
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = KalosTheme.type.metricSmall,
            color = if (isFuture && !isSelected) colors.onSurfaceVariant else ink,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(14.dp),
        ) {
            if (kcal > 0f && kcalGoal > 0f) {
                KcalGauge(fraction = kcal / kcalGoal, fill = gaugeFill, track = if (isSelected) colors.surface.copy(alpha = 0.35f) else colors.outlineVariant)
            }
            when {
                hasWorkout -> Dot(ink)
                hasPlanned -> Dot(ink, filled = false)
            }
        }
    }
}

/** Petite jauge verticale : remplie jusqu'à kcal / objectif (plafonnée à la hauteur). */
@Composable
private fun KcalGauge(fraction: Float, fill: Color, track: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 4.dp, height = 14.dp)
            .clip(KalosShape.chip)
            .drawBehind {
                drawRect(track)
                val h = size.height * fraction.coerceIn(0f, 1f)
                drawRect(fill, Offset(0f, size.height - h), Size(size.width, h))
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    val ink = MaterialTheme.colorScheme.onSurface
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendItem(stringResource(R.string.me_cal_legend_kcal)) {
            KcalGauge(fraction = 0.6f, fill = ink, track = MaterialTheme.colorScheme.outlineVariant)
        }
        LegendItem(stringResource(R.string.state_over_target).replaceFirstChar { it.uppercase() }) {
            Swatch(KalosTheme.nutrients.overTarget)
        }
        LegendItem(stringResource(R.string.me_cal_legend_done)) { Dot(ink) }
        LegendItem(stringResource(R.string.me_cal_legend_planned)) { Dot(ink, filled = false) }
    }
}

@Composable
private fun LegendItem(label: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        mark()
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Détail du jour ───────────────────────────────────────────────────────────

@Composable
private fun DayDetailCard(
    selectedDate: String,
    detail: DayDetail?,
    kcalGoal: Float,
    plannedWorkoutName: String?,
    activeProgramName: String?,
    isToday: Boolean,
    onNavigateToNutrition: () -> Unit,
) {
    val date = remember(selectedDate) { LocalDate.parse(selectedDate) }
    val dateLabel = when (date) {
        LocalDate.now() -> stringResource(R.string.today_title)
        LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
        else -> longDate(date)
    }
    KalosCard {
        SectionLabel(dateLabel)
        val kcal = detail?.totalKcal ?: 0f
        val protein = detail?.totalProtein ?: 0f
        val water = detail?.waterMl ?: 0
        val logs = detail?.workoutLogs.orEmpty()
        if (kcal <= 0f && protein <= 0f && water <= 0 && logs.isEmpty() && plannedWorkoutName == null) {
            Text(
                stringResource(R.string.me_cal_detail_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (kcal > 0f || protein > 0f) {
            if (kcalGoal > 0f) {
                MeasureRow(
                    label = stringResource(R.string.nutrition_calories),
                    value = kcal,
                    goal = kcalGoal,
                    valueText = formatGroupedInt(kcal.roundToInt()),
                    goalText = formatGroupedInt(kcalGoal.roundToInt()),
                    unit = stringResource(R.string.kcal_label),
                    spokenUnit = stringResource(R.string.unit_kcal_spoken),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                ValueLine(
                    stringResource(R.string.nutrition_calories),
                    "${formatGroupedInt(kcal.roundToInt())} ${stringResource(R.string.kcal_label)}",
                    stringResource(R.string.unit_kcal_spoken),
                    formatGroupedInt(kcal.roundToInt()),
                )
            }
            val p = formatGroupedInt(protein.roundToInt())
            ValueLine(
                stringResource(R.string.nutrition_protein),
                "$p ${stringResource(R.string.unit_g)}",
                stringResource(R.string.unit_grams_spoken),
                p,
            )
        }
        if (water > 0) {
            val w = formatGroupedInt(water)
            ValueLine(stringResource(R.string.water_title), "$w ${stringResource(R.string.unit_ml)}", stringResource(R.string.me_unit_ml_spoken), w)
        }
        if (logs.isNotEmpty()) {
            InstrumentLabel(stringResource(R.string.me_cal_section_done))
            logs.forEach { log ->
                val name = log.templateName.ifBlank { stringResource(R.string.me_cal_free_session) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(MaterialTheme.colorScheme.onSurface)
                    Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (log.durationSecs > 0) {
                        Text(
                            stringResource(R.string.me_minutes, log.durationSecs / 60),
                            style = KalosTheme.type.metricSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (plannedWorkoutName != null) {
            InstrumentLabel(stringResource(R.string.me_cal_section_planned))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dot(MaterialTheme.colorScheme.onSurface, filled = false)
                Text(
                    if (activeProgramName != null) stringResource(R.string.me_cal_planned_in_program, plannedWorkoutName, activeProgramName)
                    else plannedWorkoutName,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (isToday) {
            KalosButton(stringResource(R.string.me_cal_view_nutrition), onClick = onNavigateToNutrition, ghost = true)
        }
    }
}

/** Ligne « libellé ····· valeur », lue en une phrase (« Protéines, 82 grammes »). */
@Composable
private fun ValueLine(label: String, value: String, spokenUnit: String, spokenValue: String) {
    val sentence = stringResource(R.string.me_value_spoken, label, spokenValue, spokenUnit)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 24.dp).clearAndSetSemantics { contentDescription = sentence },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = KalosTheme.type.metricSmall)
    }
}

// ─── Aperçu ──────────────────────────────────────────────────────────────────

@Composable
private fun InsightsSection(insights: CalendarInsightsState) {
    SectionLabel(stringResource(R.string.me_cal_overview), Modifier.padding(top = 8.dp))

    KalosCard {
        InstrumentLabel(stringResource(R.string.me_cal_freq_title))
        Text(
            stringResource(R.string.me_cal_freq_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (insights.workoutsPerWeek.isNotEmpty()) WorkoutFrequency(insights.workoutsPerWeek)
    }

    if (insights.kcalGoal > 0f) {
        KalosCard {
            InstrumentLabel(stringResource(R.string.me_cal_avg_title))
            MeasureRow(
                label = stringResource(R.string.me_cal_avg_label),
                value = insights.avgKcal7d,
                goal = insights.kcalGoal,
                valueText = formatGroupedInt(insights.avgKcal7d.roundToInt()),
                goalText = formatGroupedInt(insights.kcalGoal.roundToInt()),
                unit = stringResource(R.string.kcal_label),
                spokenUnit = stringResource(R.string.unit_kcal_spoken),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.me_cal_avg_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Quatre semaines côte à côte : chiffre lisible, barre fine à l'encre, semaine en cours pleine. */
@Composable
private fun WorkoutFrequency(weeks: List<Int>) {
    val max = weeks.maxOrNull()?.coerceAtLeast(1) ?: 1
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        weeks.forEachIndexed { i, count ->
            val ago = weeks.lastIndex - i
            val spokenWeek = if (ago == 0) stringResource(R.string.me_week_current)
                             else pluralStringResource(R.plurals.me_weeks_ago, ago, ago)
            val sentence = spokenWeek + ", " + pluralStringResource(R.plurals.me_sessions, count, count)
            Column(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = sentence },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(count.toString(), style = KalosTheme.type.metric)
                Box(
                    Modifier
                        .size(width = 10.dp, height = (4 + 36f * count / max).dp)
                        .clip(KalosShape.chip)
                        .background(if (ago == 0) colors.onSurface else colors.outlineVariant),
                )
                Text(
                    if (ago == 0) stringResource(R.string.me_week_current_short) else stringResource(R.string.me_week_short, ago),
                    style = KalosTheme.type.metricSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

private fun longDate(date: LocalDate): String = date.format(LONG_DATE).replaceFirstChar { it.uppercase() }

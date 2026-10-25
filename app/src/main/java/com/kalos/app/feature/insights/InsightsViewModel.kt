package com.kalos.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.UserProfile
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import com.kalos.app.core.domain.model.DailyTotals
import com.kalos.app.core.ui.util.todayFlow
import com.kalos.app.R
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

enum class InsightsPeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
}

/** Verdict of the weight trend against the profile's goal direction. */
enum class WeightVerdict { ON_TRACK, STALLED, OPPOSITE, NEUTRAL }

data class NutritionInsight(
    val avgKcal: Int = 0,
    val avgProtein: Int = 0,
    val goalKcal: Int = 0,
    val goalProtein: Int = 0,
    val isKcalOver: Boolean = false,
    val daysWithData: Int = 0,
    val daysOnKcalTarget: Int = 0,
    val daysOnProteinTarget: Int = 0,
    /** kcal/day for each tracked day, oldest → newest, for the mini bar chart. */
    val kcalPerDay: List<Float> = emptyList(),
    /** Date (yyyy-MM-dd) of each [kcalPerDay] entry, for the chart cursor reading. */
    val kcalDates: List<String> = emptyList(),
)

data class WeightInsight(
    val hasEnoughData: Boolean = false,
    val startKg: Float = 0f,
    val currentKg: Float = 0f,
    val deltaKg: Float = 0f,
    val perWeekKg: Float = 0f,
    val verdict: WeightVerdict = WeightVerdict.NEUTRAL,
)

data class TrainingInsight(
    val sessionsThisWeek: Int = 0,
    val weeklyTarget: Int? = null,
    val sessionsInPeriod: Int = 0,
    /** Sessions per rolling week over the last 4 weeks, oldest → newest. */
    val sessionsPerWeek: List<Int> = emptyList(),
)

/** A key figure surfaced as a standalone pill in the "À retenir" card. */
data class SummaryHighlight(
    @StringRes val valueRes: Int,
    val valueArgs: List<Any>,
    @StringRes val labelRes: Int = 0,
    @PluralsRes val labelPlurals: Int = 0,
    val count: Int = 0,
)

/** The "À retenir" sentence: one phrasing picked from [pool] by [seed]. */
data class SummaryHeadline(@ArrayRes val pool: Int, val seed: Int)

data class InsightsUiState(
    val period: InsightsPeriod = InsightsPeriod.WEEK,
    val isLoading: Boolean = true,
    val nutrition: NutritionInsight = NutritionInsight(),
    val weight: WeightInsight = WeightInsight(),
    val training: TrainingInsight = TrainingInsight(),
    val summaryHeadline: SummaryHeadline? = null,
    val summaryHighlights: List<SummaryHighlight> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val _period = MutableStateFlow(InsightsPeriod.WEEK)

    private data class Data(
        val period: InsightsPeriod,
        val today: LocalDate,
        val summaries: List<DailyTotals>,
        val goal: NutritionGoal?,
        val profile: UserProfile?,
        val trainedDates: List<String>,
        val weights: List<Pair<String, Float>>,
    )

    private val dataFlow: Flow<Data> = combine(_period, todayFlow()) { p, t -> p to t }.flatMapLatest { (period, today) ->
        val start = today.minusDays((period.days - 1).toLong()).toString()
        combine(
            mealRepository.getDailySummaries(start, today.toString()),
            userRepository.observeGoal(),
            userRepository.observeProfile(),
            workoutRepository.getTrainedDates(),
            workoutRepository.getBodyWeightHistory(),
        ) { summaries, goal, profile, trainedDates, weights ->
            Data(period, today, summaries, goal, profile, trainedDates, weights)
        }
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        dataFlow,
        programRepository.getActive(),
    ) { data, activeProgram ->
        build(data, activeProgram?.daysPerWeek)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InsightsUiState(),
    )

    fun setPeriod(period: InsightsPeriod) {
        _period.value = period
    }

    private fun build(data: Data, weeklyTarget: Int?): InsightsUiState {
        val today = data.today
        val periodStart = today.minusDays((data.period.days - 1).toLong())

        // ── Nutrition ──────────────────────────────────────────────────────────
        val goalKcal = data.goal?.kcal ?: 0
        val goalProtein = data.goal?.proteinG ?: 0
        val tracked = data.summaries
            .filter { it.totalKcal > 0f }
            .sortedBy { it.date }
        val daysWithData = tracked.size
        val avgKcal = if (daysWithData == 0) 0f
            else tracked.sumOf { it.totalKcal.toDouble() }.toFloat() / daysWithData
        val avgProtein = if (daysWithData == 0) 0f
            else tracked.sumOf { it.totalProtein.toDouble() }.toFloat() / daysWithData
        // "On target": kcal within [90%, 105%] of goal; protein at or above goal.
        val daysOnKcalTarget = if (goalKcal <= 0) 0 else tracked.count {
            val k = it.totalKcal
            k in (goalKcal * NutritionGoal.ZONE_LOW)..(goalKcal * NutritionGoal.ZONE_HIGH)
        }
        val daysOnProteinTarget = if (goalProtein <= 0) 0 else tracked.count {
            it.totalProtein >= goalProtein
        }
        val nutrition = NutritionInsight(
            avgKcal = avgKcal.roundToInt(),
            avgProtein = avgProtein.roundToInt(),
            goalKcal = goalKcal,
            goalProtein = goalProtein,
            isKcalOver = goalKcal > 0 && avgKcal > goalKcal * NutritionGoal.ZONE_HIGH,
            daysWithData = daysWithData,
            daysOnKcalTarget = daysOnKcalTarget,
            daysOnProteinTarget = daysOnProteinTarget,
            kcalPerDay = tracked.map { it.totalKcal },
            kcalDates = tracked.map { it.date },
        )

        // ── Weight ─────────────────────────────────────────────────────────────
        val weightsInPeriod = data.weights
            .filter { !LocalDate.parse(it.first).isBefore(periodStart) }
            .sortedBy { it.first }
        val weight = if (weightsInPeriod.size < 2) {
            WeightInsight(hasEnoughData = false)
        } else {
            val first = weightsInPeriod.first()
            val last = weightsInPeriod.last()
            val delta = last.second - first.second
            val spanDays = ChronoUnit.DAYS.between(
                LocalDate.parse(first.first), LocalDate.parse(last.first),
            ).coerceAtLeast(1L)
            val perWeek = delta / (spanDays / 7f)
            val goalDelta = data.profile?.goal?.kcalDelta ?: 0
            val verdict = weightVerdict(goalDelta, delta)
            WeightInsight(
                hasEnoughData = true,
                startKg = first.second,
                currentKg = last.second,
                deltaKg = delta,
                perWeekKg = perWeek,
                verdict = verdict,
            )
        }

        // ── Training ───────────────────────────────────────────────────────────
        val trained = data.trainedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        val sessionsThisWeek = trained.count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }
        val sessionsInPeriod = trained.count { !it.isBefore(periodStart) && !it.isAfter(today) }
        val sessionsPerWeek = (3 downTo 0).map { weekOffset ->
            val weekEnd = today.minusDays((weekOffset * 7).toLong())
            val weekStart = weekEnd.minusDays(6)
            trained.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
        }
        val training = TrainingInsight(
            sessionsThisWeek = sessionsThisWeek,
            weeklyTarget = weeklyTarget?.takeIf { it > 0 },
            sessionsInPeriod = sessionsInPeriod,
            sessionsPerWeek = sessionsPerWeek,
        )

        return InsightsUiState(
            period = data.period,
            isLoading = false,
            nutrition = nutrition,
            weight = weight,
            training = training,
            summaryHeadline = buildHeadline(nutrition, weight, training),
            summaryHighlights = buildHighlights(nutrition, weight, training),
        )
    }

    private fun weightVerdict(goalDelta: Int, deltaKg: Float): WeightVerdict {
        val threshold = 0.15f // kg, below this we consider it flat
        return when {
            goalDelta < 0 -> when {  // aiming to lose
                deltaKg <= -threshold -> WeightVerdict.ON_TRACK
                deltaKg >= threshold -> WeightVerdict.OPPOSITE
                else -> WeightVerdict.STALLED
            }
            goalDelta > 0 -> when {  // aiming to gain
                deltaKg >= threshold -> WeightVerdict.ON_TRACK
                deltaKg <= -threshold -> WeightVerdict.OPPOSITE
                else -> WeightVerdict.STALLED
            }
            else -> if (abs(deltaKg) <= threshold) WeightVerdict.ON_TRACK else WeightVerdict.NEUTRAL
        }
    }

    private fun buildHeadline(
        n: NutritionInsight,
        w: WeightInsight,
        t: TrainingInsight,
    ): SummaryHeadline {
        if (n.daysWithData == 0 && t.sessionsInPeriod == 0) {
            return SummaryHeadline(R.array.insights_headline_empty, 0)
        }
        val onTargetRatio = if (n.daysWithData > 0) n.daysOnKcalTarget.toFloat() / n.daysWithData else 0f
        val nutritionGood = n.daysWithData > 0 && onTargetRatio >= 0.7f
        val nutritionMid = n.daysWithData > 0 && onTargetRatio in 0.4f..0.7f
        val trainingGood = if (t.weeklyTarget != null) t.sessionsThisWeek >= t.weeklyTarget
                           else t.sessionsInPeriod >= 2

        // A pool of phrasings per situation; a data-derived seed keeps the choice
        // stable for a given period but varied as the numbers evolve week to week.
        val pool = when {
            nutritionGood && trainingGood -> R.array.insights_headline_all_good
            nutritionGood && !trainingGood -> R.array.insights_headline_nutrition_only
            !nutritionGood && trainingGood -> R.array.insights_headline_training_only
            nutritionMid -> R.array.insights_headline_mid
            else -> R.array.insights_headline_uneven
        }
        return SummaryHeadline(pool, n.daysOnKcalTarget * 31 + t.sessionsInPeriod * 7 + n.daysWithData + n.avgProtein)
    }

    private fun buildHighlights(
        n: NutritionInsight,
        w: WeightInsight,
        t: TrainingInsight,
    ): List<SummaryHighlight> {
        val list = mutableListOf<SummaryHighlight>()
        if (n.daysWithData > 0) {
            list += SummaryHighlight(R.string.insights_hl_days_value, listOf(n.daysOnKcalTarget, n.daysWithData), R.string.insights_hl_days_label)
            list += SummaryHighlight(R.string.insights_hl_protein_value, listOf(n.avgProtein), R.string.insights_hl_protein_label)
        }
        list += SummaryHighlight(
            R.string.insights_hl_count, listOf(t.sessionsInPeriod),
            labelPlurals = R.plurals.insights_hl_sessions_label, count = t.sessionsInPeriod,
        )
        if (w.hasEnoughData) {
            list += SummaryHighlight(
                R.string.insights_hl_weight_value, listOf(String.format(Locale.FRENCH, "%+.1f", w.deltaKg)),
                R.string.insights_hl_weight_label,
            )
        }
        return list
    }
}

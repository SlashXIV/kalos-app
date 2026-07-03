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

enum class InsightsPeriod(val days: Int, val label: String) {
    WEEK(7, "7 jours"),
    MONTH(30, "30 jours"),
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

data class InsightsUiState(
    val period: InsightsPeriod = InsightsPeriod.WEEK,
    val isLoading: Boolean = true,
    val nutrition: NutritionInsight = NutritionInsight(),
    val weight: WeightInsight = WeightInsight(),
    val training: TrainingInsight = TrainingInsight(),
    val summary: String = "",
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
        val summaries: List<com.kalos.app.core.database.dao.DailySummaryRow>,
        val goal: NutritionGoal?,
        val profile: UserProfile?,
        val trainedDates: List<String>,
        val weights: List<Pair<String, Float>>,
    )

    private val dataFlow: Flow<Data> = _period.flatMapLatest { period ->
        val today = LocalDate.now()
        val start = today.minusDays((period.days - 1).toLong()).toString()
        combine(
            mealRepository.getDailySummaries(start, today.toString()),
            userRepository.observeGoal(),
            userRepository.observeProfile(),
            workoutRepository.getTrainedDates(),
            workoutRepository.getBodyWeightHistory(),
        ) { summaries, goal, profile, trainedDates, weights ->
            Data(period, summaries, goal, profile, trainedDates, weights)
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
        val today = LocalDate.now()
        val periodStart = today.minusDays((data.period.days - 1).toLong())

        // ── Nutrition ──────────────────────────────────────────────────────────
        val goalKcal = data.goal?.kcal ?: 0
        val goalProtein = data.goal?.proteinG ?: 0
        val tracked = data.summaries
            .filter { (it.totalKcal ?: 0f) > 0f }
            .sortedBy { it.date }
        val daysWithData = tracked.size
        val avgKcal = if (daysWithData == 0) 0f
            else tracked.sumOf { (it.totalKcal ?: 0f).toDouble() }.toFloat() / daysWithData
        val avgProtein = if (daysWithData == 0) 0f
            else tracked.sumOf { (it.totalProtein ?: 0f).toDouble() }.toFloat() / daysWithData
        // "On target": kcal within [90%, 105%] of goal; protein at or above goal.
        val daysOnKcalTarget = if (goalKcal <= 0) 0 else tracked.count {
            val k = it.totalKcal ?: 0f
            k in (goalKcal * 0.90f)..(goalKcal * 1.05f)
        }
        val daysOnProteinTarget = if (goalProtein <= 0) 0 else tracked.count {
            (it.totalProtein ?: 0f) >= goalProtein
        }
        val nutrition = NutritionInsight(
            avgKcal = avgKcal.roundToInt(),
            avgProtein = avgProtein.roundToInt(),
            goalKcal = goalKcal,
            goalProtein = goalProtein,
            isKcalOver = goalKcal > 0 && avgKcal > goalKcal * 1.05f,
            daysWithData = daysWithData,
            daysOnKcalTarget = daysOnKcalTarget,
            daysOnProteinTarget = daysOnProteinTarget,
            kcalPerDay = tracked.map { it.totalKcal ?: 0f },
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
            summary = buildSummary(data.period, nutrition, weight, training),
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

    private fun buildSummary(
        period: InsightsPeriod,
        n: NutritionInsight,
        w: WeightInsight,
        t: TrainingInsight,
    ): String {
        if (n.daysWithData == 0 && t.sessionsInPeriod == 0) {
            return "Rien à analyser pour l'instant. Logue tes repas et tes séances pour voir ton bilan."
        }
        val onTargetRatio = if (n.daysWithData > 0) n.daysOnKcalTarget.toFloat() / n.daysWithData else 0f
        val meetsTraining = t.weeklyTarget == null || t.sessionsThisWeek >= t.weeklyTarget
        val lead = when {
            onTargetRatio >= 0.8f && meetsTraining -> "Période solide."
            onTargetRatio >= 0.5f -> "Bon rythme, quelques écarts."
            else -> "Période en dents de scie."
        }

        val parts = mutableListOf<String>()
        if (n.daysWithData > 0) {
            parts += "calories dans la cible ${n.daysOnKcalTarget}/${n.daysWithData} j"
            parts += "${n.avgProtein} g de protéines/j"
        }
        parts += "${t.sessionsInPeriod} séance${if (t.sessionsInPeriod > 1) "s" else ""}"
        if (w.hasEnoughData) {
            val sign = if (w.deltaKg >= 0f) "+" else ""
            parts += "$sign${"%.1f".format(w.deltaKg)} kg"
        }
        return "$lead Sur ${period.days} jours : ${parts.joinToString(", ")}."
    }
}

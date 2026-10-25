package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.kalos.app.R
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateBmrUseCase
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import com.kalos.app.core.domain.usecase.CalculateTdeeUseCase
import com.kalos.app.core.ui.component.KalosNumberField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class EditGoalsState(
    val kcal: String = NutritionGoal.DEFAULT_KCAL.toString(),
    val protein: String = NutritionGoal.DEFAULT_PROTEIN_G.toString(),
    val carbs: String = NutritionGoal.DEFAULT_CARBS_G.toString(),
    val fat: String = NutritionGoal.DEFAULT_FAT_G.toString(),
    val hasProfile: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    // Breakdown from auto-calc
    val showBreakdown: Boolean = false,
    val bmr: Float = 0f,
    val tdee: Float = 0f,
    val goalDelta: Int = 0,
    val activityLabel: String = "",
    val goalLabel: String = "",
    // Macro breakdown detail
    val bWeightKg: Float = 0f,
    val bProteinPerKg: Float = 0f,
    val bFatPerKg: Float = 0f,
    val bProteinG: Int = 0,
    val bFatG: Int = 0,
    val bCarbsG: Int = 0,
    val bTargetKcal: Int = 0,
) {
    val isValid: Boolean get() = (kcal.toIntOrNull() ?: 0) > 0 &&
        listOf(protein, carbs, fat).all { (it.toIntOrNull() ?: -1) >= 0 }
}

@HiltViewModel
class EditGoalsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
    private val calculateTdee: CalculateTdeeUseCase,
    private val calculateBmr: CalculateBmrUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditGoalsState())
    val state: StateFlow<EditGoalsState> = _state

    init {
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            userRepository.getGoal()?.let { g ->
                _state.update {
                    it.copy(
                        kcal = g.kcal.toString(), protein = g.proteinG.toString(),
                        carbs = g.carbsG.toString(), fat = g.fatG.toString(),
                        hasProfile = profile != null,
                    )
                }
            } ?: _state.update { it.copy(hasProfile = profile != null) }
        }
    }

    fun onKcalChange(v: String) = _state.update { it.copy(kcal = v) }
    fun onProteinChange(v: String) = _state.update { it.copy(protein = v) }
    fun onCarbsChange(v: String) = _state.update { it.copy(carbs = v) }
    fun onFatChange(v: String) = _state.update { it.copy(fat = v) }

    fun autoCalculate() {
        viewModelScope.launch {
            val profile = userRepository.getProfile() ?: return@launch
            val bmr = calculateBmr(profile)
            val tdee = calculateTdee(profile)
            val goal = calculateMacroGoals(profile)
            _state.update {
                it.copy(
                    kcal = goal.kcal.toString(),
                    protein = goal.proteinG.toString(),
                    carbs = goal.carbsG.toString(),
                    fat = goal.fatG.toString(),
                    showBreakdown = true,
                    bmr = bmr,
                    tdee = tdee,
                    goalDelta = profile.goal.kcalDelta,
                    activityLabel = profile.activityLevel.label,
                    goalLabel = profile.goal.label,
                    bWeightKg = profile.weightKg,
                    bProteinPerKg = profile.goal.proteinPerKg,
                    bFatPerKg = profile.goal.fatPerKg,
                    bProteinG = goal.proteinG,
                    bFatG = goal.fatG,
                    bCarbsG = goal.carbsG,
                    bTargetKcal = goal.kcal,
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        // An empty or invalid field used to silently save the default value: refuse instead.
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            userRepository.saveGoal(
                NutritionGoal(kcal = s.kcal.toInt(), proteinG = s.protein.toInt(), carbsG = s.carbs.toInt(), fatG = s.fat.toInt(), isCustom = true)
            )
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

@Composable
fun EditGoalsScreen(
    navController: NavController,
    viewModel: EditGoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) navController.popBackStack()
    }

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
                title = stringResource(R.string.me_profile_goals),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            ) {
                IconButton(onClick = viewModel::save, enabled = !state.isSaving && state.isValid) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                }
            }

            if (state.hasProfile) {
                KalosButton(
                    stringResource(R.string.me_goals_auto),
                    onClick = viewModel::autoCalculate,
                    ghost = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Détail affiché après le calcul automatique (valeurs du ViewModel, aucun calcul ici
            // hormis la part des glucides, déjà dérivée ainsi auparavant).
            if (state.showBreakdown) {
                val kcal = stringResource(R.string.kcal_label)
                KalosCard {
                    SectionLabel(stringResource(R.string.me_goals_breakdown))
                    BreakdownRow(stringResource(R.string.me_goals_bmr), formatGroupedInt(state.bmr.roundToInt()), kcal)
                    BreakdownRow(stringResource(R.string.me_goals_activity, state.activityLabel), formatGroupedInt(state.tdee.roundToInt()), kcal)
                    val deltaStr = if (state.goalDelta >= 0) "+${formatGroupedInt(state.goalDelta)}" else formatGroupedInt(state.goalDelta)
                    BreakdownRow(stringResource(R.string.me_goals_adjust, state.goalLabel), deltaStr, kcal)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownRow(stringResource(R.string.me_goals_computed), formatGroupedInt(state.bTargetKcal), kcal, strong = true)

                    SectionLabel(stringResource(R.string.me_goals_macros_auto), Modifier.padding(top = 6.dp))
                    val wKg = state.bWeightKg.toInt()
                    val nutrients = KalosTheme.nutrients
                    MacroBreakdownRow(
                        label = stringResource(R.string.nutrition_protein),
                        formula = stringResource(R.string.me_goals_formula_per_kg, "%.1f".format(state.bProteinPerKg), wKg),
                        grams = state.bProteinG,
                        kcal = state.bProteinG * 4,
                        color = nutrients.protein,
                    )
                    MacroBreakdownRow(
                        label = stringResource(R.string.nutrition_fat),
                        formula = stringResource(R.string.me_goals_formula_per_kg, "%.1f".format(state.bFatPerKg), wKg),
                        grams = state.bFatG,
                        kcal = state.bFatG * 9,
                        color = nutrients.fat,
                    )
                    val carbsKcal = state.bTargetKcal - state.bProteinG * 4 - state.bFatG * 9
                    MacroBreakdownRow(
                        label = stringResource(R.string.nutrition_carbs),
                        formula = stringResource(R.string.me_goals_formula_rest, formatGroupedInt(carbsKcal)),
                        grams = state.bCarbsG,
                        kcal = carbsKcal,
                        color = nutrients.carbs,
                    )
                    Text(
                        stringResource(R.string.me_goals_adjust_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionLabel(stringResource(R.string.me_goals_daily_kcal), Modifier.padding(top = 8.dp))
            GoalField(stringResource(R.string.me_goals_kcal_field), state.kcal, viewModel::onKcalChange)

            SectionLabel(stringResource(R.string.me_goals_macros), Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalField(stringResource(R.string.me_goals_protein_field), state.protein, viewModel::onProteinChange, Modifier.weight(1f))
                GoalField(stringResource(R.string.me_goals_carbs_field), state.carbs, viewModel::onCarbsChange, Modifier.weight(1f))
            }
            GoalField(stringResource(R.string.me_goals_fat_field), state.fat, viewModel::onFatChange)

            Text(
                stringResource(R.string.me_goals_equivalences),
                style = KalosTheme.type.metricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            KalosButton(
                stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = !state.isSaving && state.isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MacroBreakdownRow(label: String, formula: String, grams: Int, kcal: Int, color: Color) {
    val sentence = stringResource(R.string.meal_item_spoken, label, formatGroupedInt(grams), formatGroupedInt(kcal)) + ", " + formula
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = sentence },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Swatch(color)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(formula, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.me_goals_macro_value, formatGroupedInt(grams), formatGroupedInt(kcal)),
            style = KalosTheme.type.metricSmall,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, unit: String, strong: Boolean = false) {
    val sentence = stringResource(R.string.me_value_spoken, label, value, stringResource(R.string.unit_kcal_spoken))
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = sentence },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text("$value $unit", style = if (strong) KalosTheme.type.metric else KalosTheme.type.metricSmall)
    }
}

@Composable
private fun GoalField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    KalosNumberField(
        value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

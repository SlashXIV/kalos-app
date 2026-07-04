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
import androidx.compose.ui.text.font.FontWeight
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
    val kcal: String = "2000",
    val protein: String = "150",
    val carbs: String = "200",
    val fat: String = "67",
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
)

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
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            userRepository.saveGoal(
                NutritionGoal(
                    kcal = s.kcal.toIntOrNull() ?: 2000,
                    proteinG = s.protein.toIntOrNull() ?: 150,
                    carbsG = s.carbs.toIntOrNull() ?: 200,
                    fatG = s.fat.toIntOrNull() ?: 67,
                    isCustom = true,
                )
            )
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalsScreen(
    navController: NavController,
    viewModel: EditGoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Objectifs nutritionnels") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = !state.isSaving) {
                        Icon(Icons.Filled.Check, "Enregistrer")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Auto-calculate button
            if (state.hasProfile) {
                FilledTonalButton(
                    onClick = viewModel::autoCalculate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Calculer depuis mon profil")
                }
            }

            // Breakdown card — shown after auto-calc
            if (state.showBreakdown) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // — Calorie target breakdown —
                        Text(
                            "Détail du calcul",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        BreakdownRow("Métabolisme de base (BMR)", "${state.bmr.roundToInt()} kcal")
                        BreakdownRow("Activité · ${state.activityLabel}", "${state.tdee.roundToInt()} kcal")
                        val deltaStr = if (state.goalDelta >= 0) "+${state.goalDelta}" else "${state.goalDelta}"
                        BreakdownRow("Ajustement · ${state.goalLabel}", "$deltaStr kcal")
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Objectif calculé",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${state.bTargetKcal} kcal",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // — Macro distribution breakdown —
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Text(
                            "Répartition automatique des macros",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val wKg = state.bWeightKg
                        MacroBreakdownRow(
                            label = "Protéines",
                            formula = "${"%.1f".format(state.bProteinPerKg)}g/kg × ${wKg.toInt()}kg",
                            grams = state.bProteinG,
                            kcal = state.bProteinG * 4,
                            kcalPerG = 4,
                        )
                        MacroBreakdownRow(
                            label = "Lipides",
                            formula = "${"%.1f".format(state.bFatPerKg)}g/kg × ${wKg.toInt()}kg",
                            grams = state.bFatG,
                            kcal = state.bFatG * 9,
                            kcalPerG = 9,
                        )
                        val carbsKcal = state.bTargetKcal - state.bProteinG * 4 - state.bFatG * 9
                        MacroBreakdownRow(
                            label = "Glucides",
                            formula = "reste = $carbsKcal kcal",
                            grams = state.bCarbsG,
                            kcal = carbsKcal,
                            kcalPerG = 4,
                        )

                        Text(
                            "Vous pouvez ajuster librement les valeurs ci-dessous.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()
            Text(
                "Objectif calorique quotidien",
                style = MaterialTheme.typography.titleSmall,
            )
            GoalField("Calories (kcal)", state.kcal, viewModel::onKcalChange)

            HorizontalDivider()
            Text("Macronutriments", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalField("Protéines (g)", state.protein, viewModel::onProteinChange, Modifier.weight(1f))
                GoalField("Glucides (g)", state.carbs, viewModel::onCarbsChange, Modifier.weight(1f))
            }
            GoalField("Lipides (g)", state.fat, viewModel::onFatChange)

            // Always-visible energy equivalences reminder
            EnergyEquivalencesRow()

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun EnergyEquivalencesRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "1g protéine = 4 kcal  ·  1g glucide = 4 kcal  ·  1g lipide = 9 kcal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun MacroBreakdownRow(
    label: String,
    formula: String,
    grams: Int,
    kcal: Int,
    kcalPerG: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                formula,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${grams}g  →  ${kcal} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
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

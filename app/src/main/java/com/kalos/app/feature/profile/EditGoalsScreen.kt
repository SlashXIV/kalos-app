package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditGoalsState(
    val kcal: String = "2000",
    val protein: String = "150",
    val carbs: String = "200",
    val fat: String = "67",
    val hasProfile: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class EditGoalsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
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
            val goal = calculateMacroGoals(profile)
            _state.update {
                it.copy(
                    kcal = goal.kcal.toString(),
                    protein = goal.proteinG.toString(),
                    carbs = goal.carbsG.toString(),
                    fat = goal.fatG.toString(),
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
                title = { Text("Modifier les objectifs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Objectifs caloriques et macros quotidiens",
                style = MaterialTheme.typography.titleSmall)

            if (state.hasProfile) {
                OutlinedButton(
                    onClick = viewModel::autoCalculate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Calculer depuis mon profil")
                }
            }

            GoalField("Calories (kcal)", state.kcal, viewModel::onKcalChange)
            HorizontalDivider()
            Text("Macronutriments", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalField("Protéines (g)", state.protein, viewModel::onProteinChange, Modifier.weight(1f))
                GoalField("Glucides (g)", state.carbs, viewModel::onCarbsChange, Modifier.weight(1f))
            }
            GoalField("Lipides (g)", state.fat, viewModel::onFatChange)

            Spacer(Modifier.height(8.dp))
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
private fun GoalField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier, singleLine = true,
    )
}

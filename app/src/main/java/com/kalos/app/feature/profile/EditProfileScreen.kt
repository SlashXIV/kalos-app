package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileState(
    val name: String = "",
    val age: String = "25",
    val sex: Sex = Sex.MALE,
    val heightCm: String = "175",
    val weightKg: String = "75",
    val targetWeightKg: String = "70",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: FitnessGoal = FitnessGoal.MAINTAIN,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state

    init {
        viewModelScope.launch {
            userRepository.getProfile()?.let { p ->
                _state.update {
                    it.copy(
                        name = p.name, age = p.age.toString(), sex = p.sex,
                        heightCm = p.heightCm.toString(), weightKg = p.weightKg.toString(),
                        targetWeightKg = p.targetWeightKg.toString(),
                        activityLevel = p.activityLevel, goal = p.goal,
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v) }
    fun onSexChange(v: Sex) = _state.update { it.copy(sex = v) }
    fun onHeightChange(v: String) = _state.update { it.copy(heightCm = v) }
    fun onWeightChange(v: String) = _state.update { it.copy(weightKg = v) }
    fun onTargetWeightChange(v: String) = _state.update { it.copy(targetWeightKg = v) }
    fun onActivityChange(v: ActivityLevel) = _state.update { it.copy(activityLevel = v) }
    fun onGoalChange(v: FitnessGoal) = _state.update { it.copy(goal = v) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            userRepository.saveProfile(
                UserProfile(
                    name = s.name.trim(), age = s.age.toIntOrNull() ?: 25,
                    sex = s.sex, heightCm = s.heightCm.toFloatOrNull() ?: 175f,
                    weightKg = s.weightKg.toFloatOrNull() ?: 75f,
                    targetWeightKg = s.targetWeightKg.toFloatOrNull() ?: 70f,
                    activityLevel = s.activityLevel, goal = s.goal,
                    onboardingCompleted = true,
                )
            )
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil") },
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
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileNumField("Âge", state.age, viewModel::onAgeChange, Modifier.weight(1f))
                ProfileNumField("Taille (cm)", state.heightCm, viewModel::onHeightChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileNumField("Poids (kg)", state.weightKg, viewModel::onWeightChange, Modifier.weight(1f))
                ProfileNumField("Objectif (kg)", state.targetWeightKg, viewModel::onTargetWeightChange, Modifier.weight(1f))
            }

            Text("Sexe", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(Sex.MALE to "Homme", Sex.FEMALE to "Femme").forEachIndexed { i, (sex, label) ->
                    SegmentedButton(
                        selected = state.sex == sex,
                        onClick = { viewModel.onSexChange(sex) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }

            Text("Niveau d'activité", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActivityLevel.values().forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RadioButton(
                        selected = state.activityLevel == level,
                        onClick = { viewModel.onActivityChange(level) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(level.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("Objectif", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FitnessGoal.values().forEach { goal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RadioButton(
                        selected = state.goal == goal,
                        onClick = { viewModel.onGoalChange(goal) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(goal.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileNumField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier, singleLine = true,
    )
}

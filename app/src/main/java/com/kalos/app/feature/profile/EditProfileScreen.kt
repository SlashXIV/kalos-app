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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.kalos.app.R
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.CalculateMacroGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kalos.app.core.ui.util.parseDecimal

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
    private val calculateMacroGoals: CalculateMacroGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state

    private var originalFitnessGoal: FitnessGoal? = null

    init {
        viewModelScope.launch {
            userRepository.getProfile()?.let { p ->
                originalFitnessGoal = p.goal
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
            val profile = UserProfile(
                name = s.name.trim(), age = s.age.toIntOrNull() ?: 25,
                sex = s.sex, heightCm = s.heightCm.parseDecimal() ?: 175f,
                weightKg = s.weightKg.parseDecimal() ?: 75f,
                targetWeightKg = s.targetWeightKg.parseDecimal() ?: 70f,
                activityLevel = s.activityLevel, goal = s.goal,
                onboardingCompleted = true,
            )
            userRepository.saveProfile(profile)

            // Resync nutrition goal when the fitness objective or activity level changed,
            // or when no custom goal exists yet.
            val fitnessGoalChanged = s.goal != originalFitnessGoal
            val existingGoal = userRepository.getGoal()
            if (fitnessGoalChanged || existingGoal == null || !existingGoal.isCustom) {
                userRepository.saveGoal(calculateMacroGoals(profile))
            }

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
                title = stringResource(R.string.me_profile_nav_edit),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            ) {
                IconButton(onClick = viewModel::save, enabled = !state.isSaving) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                }
            }

            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.me_edit_first_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileNumField(stringResource(R.string.me_edit_age), state.age, viewModel::onAgeChange, Modifier.weight(1f))
                ProfileNumField(stringResource(R.string.me_edit_height), state.heightCm, viewModel::onHeightChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileNumField(stringResource(R.string.me_weight_field), state.weightKg, viewModel::onWeightChange, Modifier.weight(1f))
                ProfileNumField(stringResource(R.string.me_edit_target_weight), state.targetWeightKg, viewModel::onTargetWeightChange, Modifier.weight(1f))
            }

            SectionLabel(stringResource(R.string.me_edit_sex), Modifier.padding(top = 8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(Sex.MALE to R.string.me_sex_male, Sex.FEMALE to R.string.me_sex_female).forEachIndexed { i, (sex, label) ->
                    SegmentedButton(
                        selected = state.sex == sex,
                        onClick = { viewModel.onSexChange(sex) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2, KalosShape.control),
                        icon = {},
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { FitText(stringResource(label)) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.me_edit_activity), Modifier.padding(top = 8.dp))
            Column(Modifier.selectableGroup()) {
                ActivityLevel.entries.forEach { level ->
                    ChoiceRow(
                        selected = state.activityLevel == level,
                        title = level.label,
                        subtitle = level.description,
                        onSelect = { viewModel.onActivityChange(level) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.me_edit_goal), Modifier.padding(top = 8.dp))
            Column(Modifier.selectableGroup()) {
                FitnessGoal.entries.forEach { goal ->
                    ChoiceRow(
                        selected = state.goal == goal,
                        title = goal.label,
                        onSelect = { viewModel.onGoalChange(goal) },
                    )
                }
            }
        }
    }
}

/** Choix exclusif : toute la ligne est la cible (≥ 48 dp), lue comme bouton radio. */
@Composable
private fun ChoiceRow(selected: Boolean, title: String, subtitle: String? = null, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    KalosNumberField(
        value = value, onValueChange = onChange, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

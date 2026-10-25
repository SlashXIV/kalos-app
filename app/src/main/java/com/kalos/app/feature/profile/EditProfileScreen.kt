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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.ui.component.KalosNumberField
import kotlinx.coroutines.flow.*

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

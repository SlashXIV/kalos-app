package com.kalos.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.navigation.Screen

@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Votre profil", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ces informations permettent de calculer vos besoins caloriques personnalisés.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Votre prénom (optionnel)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Sex selector
        Text("Sexe", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.sex == Sex.MALE,
                onClick = { viewModel.onSexChange(Sex.MALE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text("Homme") }
            SegmentedButton(
                selected = state.sex == Sex.FEMALE,
                onClick = { viewModel.onSexChange(Sex.FEMALE) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text("Femme") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KalosNumberField(
                value = state.age,
                onValueChange = viewModel::onAgeChange,
                label = { Text("Âge") },
                suffix = { Text("ans") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            KalosNumberField(
                value = state.heightCm,
                onValueChange = viewModel::onHeightChange,
                label = { Text("Taille") },
                suffix = { Text("cm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KalosNumberField(
                value = state.weightKg,
                onValueChange = viewModel::onWeightChange,
                label = { Text("Poids actuel") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            KalosNumberField(
                value = state.targetWeightKg,
                onValueChange = viewModel::onTargetWeightChange,
                label = { Text("Poids cible") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { navController.navigate(Screen.GoalSetup.route) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = state.age.isNotEmpty() && state.heightCm.isNotEmpty() && state.weightKg.isNotEmpty(),
        ) {
            Text("Continuer")
        }
    }
}

package com.kalos.app.feature.nutrition.custom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodScreen(
    navController: NavController,
    foodId: Long,
    viewModel: CustomFoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(foodId) { if (foodId > 0) viewModel.loadFood(foodId) }
    LaunchedEffect(state.savedSuccessfully) { if (state.savedSuccessfully) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Modifier l'aliment" else "Nouvel aliment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
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
            Text("Informations générales", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nom *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text("Marque (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()
            Text("Valeurs nutritionnelles pour 100g/100ml", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField("Calories (kcal) *", state.kcal, viewModel::onKcalChange, Modifier.weight(1f))
                NumField("Protéines (g) *", state.protein, viewModel::onProteinChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField("Glucides (g) *", state.carbs, viewModel::onCarbsChange, Modifier.weight(1f))
                NumField("Lipides (g) *", state.fat, viewModel::onFatChange, Modifier.weight(1f))
            }
            NumField("Fibres (g)", state.fiber, viewModel::onFiberChange)

            HorizontalDivider()
            Text("Portion de référence", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField("Quantité", state.serving, viewModel::onServingChange, Modifier.weight(1f))
                OutlinedTextField(
                    value = state.unit,
                    onValueChange = {},
                    label = { Text("Unité") },
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            HorizontalDivider()
            Text("Compatibilité alimentaire", style = MaterialTheme.typography.titleSmall)
            Text(
                "Optionnel — permet au filtre de préférences de classer correctement cet aliment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DietaryCheckRow("Contient du porc", state.containsPork, viewModel::onContainsPorkChange)
            DietaryCheckRow("Contient de l'alcool", state.containsAlcohol, viewModel::onContainsAlcoholChange)
            DietaryCheckRow("Convient aux végétariens", state.isVegetarian, viewModel::onIsVegetarianChange)
            DietaryCheckRow("Convient aux végans", state.isVegan, viewModel::onIsVeganChange)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
    )
}

@Composable
private fun DietaryCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

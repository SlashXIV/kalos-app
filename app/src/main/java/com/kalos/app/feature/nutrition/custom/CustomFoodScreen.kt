package com.kalos.app.feature.nutrition.custom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosShape
import androidx.compose.ui.platform.LocalContext

@Composable
fun CustomFoodScreen(
    navController: NavController,
    foodId: Long,
    barcode: String = "",
    viewModel: CustomFoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(foodId) { if (foodId > 0) viewModel.loadFood(foodId) }
    // Creation from a scan of an unknown product: pre-fill the barcode so it gets
    // persisted on save and the product is cached for next time.
    LaunchedEffect(barcode) { viewModel.setBarcode(barcode) }
    LaunchedEffect(state.savedSuccessfully) { if (state.savedSuccessfully) navController.popBackStack() }
    LaunchedEffect(state.deletedSuccessfully) { if (state.deletedSuccessfully) navController.popBackStack() }
    val context = LocalContext.current
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            viewModel.onErrorShown()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.food_delete_title)) },
            text = { Text(stringResource(R.string.food_delete_body)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                    Text(stringResource(R.string.food_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    state.duplicateFood?.let { duplicate ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDuplicate,
            title = { Text(stringResource(R.string.food_duplicate_title)) },
            text = { Text(stringResource(R.string.food_duplicate_body, duplicate.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmSaveAnyway) { Text(stringResource(R.string.food_duplicate_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDuplicate) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
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
                title = stringResource(if (state.isEditing) R.string.food_custom_title_edit else R.string.food_custom_title_new),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            ) {
                if (state.isEditing) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.food_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.food_section_general), Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.food_field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = KalosShape.control,
            )
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text(stringResource(R.string.food_field_brand)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = KalosShape.control,
            )

            SectionLabel(stringResource(R.string.food_section_values), Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField(stringResource(R.string.food_field_kcal), state.kcal, viewModel::onKcalChange, Modifier.weight(1f))
                NumField(stringResource(R.string.food_field_protein), state.protein, viewModel::onProteinChange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField(stringResource(R.string.food_field_carbs), state.carbs, viewModel::onCarbsChange, Modifier.weight(1f))
                NumField(stringResource(R.string.food_field_fat), state.fat, viewModel::onFatChange, Modifier.weight(1f))
            }
            NumField(stringResource(R.string.food_field_fiber), state.fiber, viewModel::onFiberChange)

            SectionLabel(stringResource(R.string.food_section_serving), Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField(stringResource(R.string.food_quantity), state.serving, viewModel::onServingChange, Modifier.weight(1f))
                OutlinedTextField(
                    value = state.unit,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.food_field_unit)) },
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = KalosShape.control,
                )
            }

            SectionLabel(stringResource(R.string.food_section_diet), Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.food_diet_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                DietaryCheckRow(stringResource(R.string.food_diet_pork), state.containsPork, viewModel::onContainsPorkChange)
                DietaryCheckRow(stringResource(R.string.food_diet_alcohol), state.containsAlcohol, viewModel::onContainsAlcoholChange)
                DietaryCheckRow(stringResource(R.string.food_diet_vegetarian), state.isVegetarian, viewModel::onIsVegetarianChange)
                DietaryCheckRow(stringResource(R.string.food_diet_vegan), state.isVegan, viewModel::onIsVeganChange)
            }

            KalosButton(
                stringResource(if (state.isSaving) R.string.action_saving else R.string.action_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !state.isSaving,
            )
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    KalosNumberField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Toute la ligne bascule la case : cible de 48 dp, lue comme une case à cocher. */
@Composable
private fun DietaryCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

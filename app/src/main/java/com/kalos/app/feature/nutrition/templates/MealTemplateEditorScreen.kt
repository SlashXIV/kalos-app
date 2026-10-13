package com.kalos.app.feature.nutrition.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.feature.nutrition.search.PICKED_FOOD_ID_KEY
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class EditorItem(val food: Food, val amountG: Float) {
    val kcal: Float get() = food.kcalForAmount(amountG)
}

data class MealTemplateEditorUiState(
    val id: Long = -1L,
    val name: String = "",
    val items: List<EditorItem> = emptyList(),
    val isLoading: Boolean = true,
    val saved: Boolean = false,
) {
    val isNew: Boolean get() = id <= 0L
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty()
    val totalKcal: Float get() = items.sumOf { it.kcal.toDouble() }.toFloat()
}

@HiltViewModel
class MealTemplateEditorViewModel @Inject constructor(
    private val repository: MealTemplateRepository,
    private val foodRepository: FoodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialId: Long = savedStateHandle["templateId"] ?: -1L

    private val _state = MutableStateFlow(MealTemplateEditorUiState(id = initialId))
    val state: StateFlow<MealTemplateEditorUiState> = _state.asStateFlow()

    init {
        if (initialId > 0L) {
            viewModelScope.launch {
                val template = repository.getTemplate(initialId)
                _state.update { s ->
                    if (template == null) s.copy(isLoading = false)
                    else s.copy(
                        name = template.name,
                        items = template.items.map { EditorItem(it.food, it.amountG) },
                        isLoading = false,
                    )
                }
            }
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onAmountChange(index: Int, grams: Float) {
        _state.update { s ->
            s.copy(items = s.items.mapIndexed { i, item -> if (i == index) item.copy(amountG = grams) else item })
        }
    }

    fun removeItem(index: Int) {
        _state.update { s -> s.copy(items = s.items.filterIndexed { i, _ -> i != index }) }
    }

    fun addFood(foodId: Long) {
        viewModelScope.launch {
            val food = foodRepository.getById(foodId) ?: return@launch
            _state.update { s ->
                // Already present → leave it (user adjusts grams inline) rather than duplicate.
                if (s.items.any { it.food.id == foodId }) s
                else {
                    val defaultG = food.defaultServingG.takeIf { it > 0f } ?: 100f
                    s.copy(items = s.items + EditorItem(food, defaultG))
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val idArg = if (s.id > 0L) s.id else 0L
        viewModelScope.launch {
            val newId = repository.saveTemplate(idArg, s.name.trim(), s.items.map { it.food.id to it.amountG })
            _state.update { it.copy(id = newId, saved = true) }
        }
    }
}

@Composable
fun MealTemplateEditorScreen(
    navController: NavController,
    viewModel: MealTemplateEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) navController.popBackStack()
    }

    // Food returned by the picker (FoodSearch in pick mode).
    val currentEntry = navController.currentBackStackEntry
    LaunchedEffect(currentEntry) {
        currentEntry?.savedStateHandle
            ?.getStateFlow<Long?>(PICKED_FOOD_ID_KEY, null)
            ?.collect { foodId ->
                if (foodId != null) {
                    viewModel.addFood(foodId)
                    currentEntry.savedStateHandle[PICKED_FOOD_ID_KEY] = null
                }
            }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(if (state.isNew) R.string.food_template_new_title else R.string.food_template_edit_title),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                )
            }

            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.favorite_name)) },
                    singleLine = true,
                    shape = KalosShape.control,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "total") {
                Text(
                    pluralStringResource(
                        R.plurals.food_template_total, state.items.size,
                        formatGroupedInt(state.totalKcal.roundToInt()), state.items.size,
                    ),
                    style = KalosTheme.type.metric,
                )
            }

            if (state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.food_template_empty_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(state.items, key = { _, item -> item.food.id }) { index, item ->
                    ItemRow(
                        item = item,
                        onAmountChange = { viewModel.onAmountChange(index, it) },
                        onRemove = { viewModel.removeItem(index) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KalosButton(
                        stringResource(R.string.nutrition_add_food),
                        onClick = { navController.navigate(Screen.FoodSearch.pick()) },
                        modifier = Modifier.fillMaxWidth(),
                        ghost = true,
                    )
                    KalosButton(
                        stringResource(R.string.action_save),
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: EditorItem,
    onAmountChange: (Float) -> Unit,
    onRemove: () -> Unit,
) {
    // Local text buffer so partial edits (empty, trailing separator) don't fight the model.
    var text by remember(item.food.id) { mutableStateOf(item.amountG.roundToInt().toString()) }
    val kcal = formatGroupedInt(item.kcal.roundToInt())
    val spoken = stringResource(R.string.food_item_kcal_spoken, item.food.name, kcal)
    val amountDesc = stringResource(R.string.food_item_amount_desc, item.food.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken }) {
            Text(item.food.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.food_kcal_value, kcal),
                style = KalosTheme.type.metricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        KalosNumberField(
            value = text,
            onValueChange = {
                text = it
                it.replace(',', '.').toFloatOrNull()?.let(onAmountChange)
            },
            suffix = { Text(stringResource(R.string.unit_g)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(110.dp).semantics { contentDescription = amountDesc },
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.meal_item_remove, item.food.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

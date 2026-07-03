package com.kalos.app.feature.nutrition.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Nouveau repas favori" else "Modifier le favori") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave) {
                        Text("Enregistrer")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nom du repas favori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "total") {
                Text(
                    "${state.totalKcal.roundToInt()} kcal · ${state.items.size} aliment${if (state.items.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Aucun aliment. Ajoutez-en avec le bouton ci-dessous.",
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
                    HorizontalDivider()
                }
            }

            item(key = "add") {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.FoodSearch.pick()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un aliment")
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.food.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                "${item.kcal.roundToInt()} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                it.replace(',', '.').toFloatOrNull()?.let(onAmountChange)
            },
            suffix = { Text("g") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.width(110.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Retirer",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

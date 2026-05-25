package com.kalos.app.feature.nutrition.myfoods

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.FoodListItem
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyFoodsUiState(
    val foods: List<Food> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class MyFoodsViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _allFoods = foodRepository.getCustomFoods()

    val state: StateFlow<MyFoodsUiState> = combine(_allFoods, _query) { foods, query ->
        val filtered = if (query.isBlank()) foods
        else foods.filter { it.name.contains(query, ignoreCase = true) || it.brand.contains(query, ignoreCase = true) }
        MyFoodsUiState(foods = filtered, query = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyFoodsUiState())

    fun onQueryChange(v: String) { _query.value = v }

    fun delete(food: Food) {
        viewModelScope.launch { foodRepository.archiveOrDelete(food.id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFoodsScreen(
    navController: NavController,
    viewModel: MyFoodsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Food?>(null) }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer l'aliment") },
            text = { Text("« ${pendingDelete!!.name} » sera supprimé. S'il a été utilisé dans votre historique, il sera archivé et n'apparaîtra plus dans la recherche.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(pendingDelete!!); pendingDelete = null }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes aliments") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.CustomFood.create()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Créer un aliment")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Rechercher dans mes aliments…",
            )
            Spacer(Modifier.height(8.dp))

            if (state.foods.isEmpty()) {
                EmptyState(
                    title = if (state.query.isBlank()) "Aucun aliment personnalisé" else "Aucun résultat",
                    subtitle = if (state.query.isBlank()) "Créez votre premier aliment avec le bouton +" else "Essayez un autre terme",
                )
            } else {
                LazyColumn {
                    items(state.foods, key = { it.id }) { food ->
                        FoodListItem(
                            food = food,
                            onClick = { navController.navigate(Screen.CustomFood.edit(food.id)) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { navController.navigate(Screen.CustomFood.edit(food.id)) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { pendingDelete = food }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

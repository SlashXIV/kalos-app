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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.FoodListItem
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.navigation.Screen
import kotlinx.coroutines.flow.*

@Composable
fun MyFoodsScreen(
    navController: NavController,
    viewModel: MyFoodsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Food?>(null) }

    pendingDelete?.let { food ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.food_delete_title)) },
            text = { Text(stringResource(R.string.food_delete_named_body, food.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(food); pendingDelete = null }) {
                    Text(stringResource(R.string.food_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.food_my_foods),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            ) {
                IconButton(onClick = { navController.navigate(Screen.CustomFood.create()) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.food_create_action))
                }
            }
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.food_my_search_placeholder),
            )
            Spacer(Modifier.height(8.dp))

            if (state.foods.isEmpty()) {
                EmptyState(
                    title = stringResource(if (state.query.isBlank()) R.string.food_my_empty_title else R.string.food_no_result),
                    subtitle = stringResource(if (state.query.isBlank()) R.string.food_my_empty_subtitle else R.string.food_try_other),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(state.foods, key = { it.id }) { food ->
                        FoodListItem(
                            food = food,
                            onClick = { navController.navigate(Screen.CustomFood.edit(food.id)) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { navController.navigate(Screen.CustomFood.edit(food.id)) }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.food_edit_named, food.name),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { pendingDelete = food }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.food_delete_named, food.name),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

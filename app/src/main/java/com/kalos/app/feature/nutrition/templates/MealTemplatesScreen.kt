package com.kalos.app.feature.nutrition.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.MealTemplate
import com.kalos.app.core.domain.repository.MealTemplateRepository
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.MeasureTicket
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.TicketLine
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class MealTemplatesViewModel @Inject constructor(
    private val repository: MealTemplateRepository,
) : ViewModel() {

    val templates: StateFlow<List<MealTemplate>> = repository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}

@Composable
fun MealTemplatesScreen(
    navController: NavController,
    viewModel: MealTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<MealTemplate?>(null) }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.food_template_delete_title)) },
            text = { Text(stringResource(R.string.food_template_delete_body, template.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(template.id); pendingDelete = null }) {
                    Text(stringResource(R.string.food_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.food_templates_title),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                ) {
                    IconButton(onClick = { navController.navigate(Screen.MealTemplateEdit.create()) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.food_template_create))
                    }
                }
            }
            if (templates.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.food_templates_empty_title),
                        subtitle = stringResource(R.string.food_templates_empty_subtitle),
                    )
                }
            }
            items(templates, key = { it.id }) { template ->
                TemplateTicket(
                    template = template,
                    onEdit = { navController.navigate(Screen.MealTemplateEdit.edit(template.id)) },
                    onDelete = { pendingDelete = template },
                )
            }
        }
    }
}

/** Un repas favori en ticket de pesée : nom et total, puis une ligne par aliment. */
@Composable
private fun TemplateTicket(template: MealTemplate, onEdit: () -> Unit, onDelete: () -> Unit) {
    val g = stringResource(R.string.unit_g)
    MeasureTicket(Modifier.clip(KalosShape.card).clickable(onClickLabel = stringResource(R.string.food_template_edit_action), onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(
                        R.plurals.food_template_total, template.items.size,
                        formatGroupedInt(template.totalKcal.roundToInt()), template.items.size,
                    ),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.food_delete_named, template.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        template.items.forEach { item ->
            val amount = formatGroupedInt(item.amountG.roundToInt())
            val kcal = formatGroupedInt(item.food.kcalForAmount(item.amountG).roundToInt())
            TicketLine(
                name = item.food.name,
                amount = "$amount $g",
                value = kcal,
                spoken = stringResource(R.string.meal_item_spoken, item.food.name, amount, kcal),
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

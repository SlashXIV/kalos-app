package com.kalos.app.feature.nutrition.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.MealTemplate
import com.kalos.app.core.domain.repository.MealTemplateRepository
import com.kalos.app.core.ui.component.EmptyState
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

@OptIn(ExperimentalMaterial3Api::class)
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
            title = { Text("Supprimer le repas favori") },
            text = { Text("« ${template.name} » sera supprimé. Les repas déjà enregistrés dans votre journal ne sont pas affectés.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(template.id); pendingDelete = null }) {
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
                title = { Text("Repas favoris") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "Aucun repas favori",
                    subtitle = "Depuis un repas déjà rempli, ouvrez le menu (⋮) et choisissez « Enregistrer comme favori ».",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(templates, key = { it.id }) { template ->
                    ListItem(
                        headlineContent = {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            val foods = template.items.joinToString(", ") {
                                "${it.food.name} (${it.amountG.roundToInt()} g)"
                            }
                            Text(
                                "${template.totalKcal.roundToInt()} kcal · $foods",
                                maxLines = 2,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = template }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

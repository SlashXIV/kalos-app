package com.kalos.app.feature.workout.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val exercises: List<Exercise> = emptyList(),
    val query: String = "",
    val selectedMuscle: String = "",
    val selectedType: String = "",
    val selectedEquipment: String = "",
    val onlyFavorites: Boolean = false,
    val muscleGroups: List<String> = emptyList(),
    val equipmentTypes: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseCatalogViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _displayQuery = MutableStateFlow("") // immediate, drives the text field
    private val _muscle = MutableStateFlow("")
    private val _type = MutableStateFlow("")
    private val _equipment = MutableStateFlow("")
    private val _onlyFavorites = MutableStateFlow(false)
    private val _muscleGroups = MutableStateFlow<List<String>>(emptyList())
    private val _equipmentTypes = MutableStateFlow<List<String>>(emptyList())

    private data class Filters(val query: String, val muscle: String, val type: String, val equipment: String, val onlyFavorites: Boolean)

    private val filters = combine(_query.debounce(300), _muscle, _type, _equipment, _onlyFavorites) { q, m, t, e, fav ->
        Filters(q, m, t, e, fav)
    }

    val uiState: StateFlow<CatalogUiState> = combine(
        filters.flatMapLatest { f ->
            exerciseRepository.filter(f.query, f.muscle, f.type, f.equipment, f.onlyFavorites)
        },
        filters,
        _displayQuery,
        _muscleGroups,
        _equipmentTypes,
    ) { exercises, f, displayQuery, muscles, equipment ->
        CatalogUiState(
            exercises = exercises,
            query = displayQuery,
            selectedMuscle = f.muscle,
            selectedType = f.type,
            selectedEquipment = f.equipment,
            onlyFavorites = f.onlyFavorites,
            muscleGroups = muscles,
            equipmentTypes = equipment,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogUiState(),
    )

    init {
        viewModelScope.launch {
            _muscleGroups.value = exerciseRepository.getMuscleGroups()
            _equipmentTypes.value = exerciseRepository.getEquipmentTypes()
        }
    }

    fun onQueryChange(v: String) {
        _displayQuery.value = v
        _query.value = v
    }
    fun onMuscleChange(v: String) { _muscle.value = if (_muscle.value == v) "" else v }
    fun onTypeChange(v: String) { _type.value = if (_type.value == v) "" else v }
    fun onFavoritesToggle() { _onlyFavorites.value = !_onlyFavorites.value }
    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.setFavorite(exercise.id, !exercise.isFavorite)
        }
    }
    fun clearFilters() {
        _displayQuery.value = ""
        _query.value = ""
        _muscle.value = ""
        _type.value = ""
        _equipment.value = ""
        _onlyFavorites.value = false
    }
}

package com.kalos.app.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.data.util.normalizeForSearch
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _muscleFilter = MutableStateFlow("")

    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Exercise>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else exerciseRepository.filter(query = q.normalizeForSearch(), muscle = "", type = "", equipment = "")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<Exercise>> = _muscleFilter
        .flatMapLatest { muscle ->
            if (muscle.isBlank()) flowOf(emptyList())
            else exerciseRepository.filter(query = "", muscle = muscle, type = "", equipment = "")
                .map { it.take(5) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(v: String) { _query.value = v }
    fun clearQuery() { _query.value = "" }
    fun setMuscleFilter(muscle: String) {
        _muscleFilter.value = muscle
        _query.value = ""
    }
}

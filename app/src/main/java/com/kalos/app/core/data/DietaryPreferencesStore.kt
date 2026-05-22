package com.kalos.app.core.data

import android.content.Context
import com.kalos.app.core.domain.model.DietaryFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietaryPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("dietary_prefs", Context.MODE_PRIVATE)

    private val _filters = MutableStateFlow(loadFromPrefs())
    val filtersFlow: StateFlow<Set<DietaryFilter>> = _filters.asStateFlow()

    val activeFilters: Set<DietaryFilter> get() = _filters.value

    fun isEnabled(filter: DietaryFilter): Boolean = filter in _filters.value

    fun setFilter(filter: DietaryFilter, enabled: Boolean) {
        val next = _filters.value.toMutableSet()
        if (enabled) {
            next.add(filter)
            if (filter == DietaryFilter.VEGAN) next.add(DietaryFilter.VEGETARIAN)
        } else {
            next.remove(filter)
            if (filter == DietaryFilter.VEGETARIAN) next.remove(DietaryFilter.VEGAN)
        }
        persistAll(next)
        _filters.value = next
    }

    private fun persistAll(filters: Set<DietaryFilter>) {
        prefs.edit()
            .putBoolean("no_pork", DietaryFilter.NO_PORK in filters)
            .putBoolean("no_alcohol", DietaryFilter.NO_ALCOHOL in filters)
            .putBoolean("vegetarian", DietaryFilter.VEGETARIAN in filters)
            .putBoolean("vegan", DietaryFilter.VEGAN in filters)
            .apply()
    }

    private fun loadFromPrefs(): Set<DietaryFilter> = buildSet {
        if (prefs.getBoolean("no_pork", false)) add(DietaryFilter.NO_PORK)
        if (prefs.getBoolean("no_alcohol", false)) add(DietaryFilter.NO_ALCOHOL)
        if (prefs.getBoolean("vegetarian", false)) add(DietaryFilter.VEGETARIAN)
        if (prefs.getBoolean("vegan", false)) add(DietaryFilter.VEGAN)
    }
}

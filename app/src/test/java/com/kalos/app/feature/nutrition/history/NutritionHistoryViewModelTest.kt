package com.kalos.app.feature.nutrition.history

import com.kalos.app.core.domain.model.DailyTotals
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.repository.MealRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Le ViewModel de bout en bout : repository simulé, flux réels, Main remplacé par un dispatcher de test. */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionHistoryViewModelTest {

    private class FakeMeals(private val days: List<LocalDate>) : MealRepository {
        override fun getDailySummaries(startDate: String, endDate: String): Flow<List<DailyTotals>> = flowOf(
            days.map { it.toString() }.filter { it in startDate..endDate }.map { DailyTotals(it, 1800f, 120f, 200f, 60f) },
        )
        override suspend fun getEarliestMealDate() = days.minOrNull()?.toString()
        override suspend fun getLatestMealDate() = days.maxOrNull()?.toString()
        override fun getMealsForDate(date: String): Flow<List<MealEntry>> = flowOf(emptyList())
        override suspend fun getOrCreateMealEntry(date: String, mealType: MealType) = 0L
        override suspend fun addItemToMeal(mealEntryId: Long, item: MealItem) = Unit
        override suspend fun removeItem(itemId: Long) = Unit
        override suspend fun removeItems(itemIds: List<Long>) = Unit
        override fun getLoggedDates(): Flow<List<String>> = flowOf(emptyList())
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun afterABreakTheHistoryOpensOnTheLastMeals() = runTest {
        val today = LocalDate.now()
        val vm = NutritionHistoryViewModel(FakeMeals(listOf(today.minusDays(120), today.minusDays(66), today.minusDays(65))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value
        assertEquals(listOf(today.minusDays(65), today.minusDays(66)).map { it.toString() }, state.summaries.map { it.date })
        assertTrue("des repas plus anciens restent à charger", state.canLoadMore)
    }

    @Test
    fun recentMealsKeepThirtyDays() = runTest {
        val today = LocalDate.now()
        val vm = NutritionHistoryViewModel(FakeMeals(listOf(today.minusDays(40), today.minusDays(2))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(listOf(today.minusDays(2).toString()), vm.uiState.value.summaries.map { it.date })
        assertTrue(vm.uiState.value.canLoadMore)
    }
}

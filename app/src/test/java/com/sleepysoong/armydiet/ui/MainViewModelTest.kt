package com.sleepysoong.armydiet.ui

import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.domain.MealDataRepository
import com.sleepysoong.armydiet.testutil.FakeAppClock
import com.sleepysoong.armydiet.testutil.FakeAppSettings
import com.sleepysoong.armydiet.testutil.MainDispatcherRule
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_withoutApiKeyAndEndpoint_showsSourceSelection() = runTest {
        val repository = FakeMealDataRepository()
        val viewModel = createViewModel(
            repository = repository,
            settings = FakeAppSettings(apiKey = null, externalApiEndpoint = null)
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MealUiState.SourceSelection)
        assertTrue(repository.requestedDates.isEmpty())
    }

    @Test
    fun init_localSourceWithoutApiKey_showsApiKeyMissing() = runTest {
        val viewModel = createViewModel(
            settings = FakeAppSettings(
                apiKey = null,
                externalApiEndpoint = "https://configured.example",
                mealSource = AppPreferences.SOURCE_LOCAL
            )
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MealUiState.ApiKeyMissing)
    }

    @Test
    fun init_externalSourceWithoutEndpoint_showsExternalEndpointMissing() = runTest {
        val viewModel = createViewModel(
            settings = FakeAppSettings(
                apiKey = "api-key",
                externalApiEndpoint = null,
                mealSource = AppPreferences.SOURCE_EXTERNAL
            )
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MealUiState.ExternalEndpointMissing)
    }

    @Test
    fun init_afterSixPmLoadsTomorrowAndMarksAsDefaultDate() = runTest {
        val repository = FakeMealDataRepository().apply {
            mealByDate["20260316"] = meal("20260316")
        }
        val widgetUpdates = RecordingWidgetUpdateDispatcher()
        val viewModel = createViewModel(
            repository = repository,
            settings = FakeAppSettings(apiKey = "api-key"),
            clock = FakeAppClock(
                nowMillis = 10_000L,
                currentDate = LocalDate.of(2026, 3, 15),
                currentTime = LocalTime.of(19, 0)
            ),
            widgetUpdateDispatcher = widgetUpdates
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as MealUiState.Success
        assertEquals(listOf("20260316"), repository.requestedDates)
        assertTrue(state.isDefaultDate)
        assertEquals(1, widgetUpdates.count)
    }

    @Test
    fun dateNavigation_movesBetweenDatesAndCanReturnToDefault() = runTest {
        val repository = FakeMealDataRepository().apply {
            mealByDate["20260315"] = meal("20260315")
            mealByDate["20260314"] = meal("20260314")
        }
        val clock = FakeAppClock(
            nowMillis = 20_000L,
            currentDate = LocalDate.of(2026, 3, 15),
            currentTime = LocalTime.of(10, 0)
        )
        val viewModel = createViewModel(
            repository = repository,
            settings = FakeAppSettings(apiKey = "api-key"),
            clock = clock
        )

        advanceUntilIdle()
        viewModel.loadPreviousMeal()
        advanceUntilIdle()

        val previousState = viewModel.uiState.value as MealUiState.Success
        assertEquals("20260314", repository.requestedDates.last())
        assertTrue(!previousState.isDefaultDate)

        viewModel.loadDefaultMeal()
        advanceUntilIdle()

        val defaultState = viewModel.uiState.value as MealUiState.Success
        assertEquals("20260315", repository.requestedDates.last())
        assertTrue(defaultState.isDefaultDate)
    }

    @Test
    fun missingLocalMeal_triggersSyncAndRetryWithForceResetOnFirstSync() = runTest {
        val repository = FakeMealDataRepository().apply {
            getMealHandler = { date ->
                requestedDates += date
                val callCount = requestedDates.count { it == date }
                if (callCount == 1) {
                    Result.success(null)
                } else {
                    Result.success(mealByDate[date])
                }
            }
            mealByDate["20260315"] = meal("20260315")
        }
        val widgetUpdates = RecordingWidgetUpdateDispatcher()
        val viewModel = createViewModel(
            repository = repository,
            settings = FakeAppSettings(apiKey = "api-key", lastCheckedIndex = 0),
            clock = FakeAppClock(
                nowMillis = 30_000L,
                currentDate = LocalDate.of(2026, 3, 15),
                currentTime = LocalTime.of(11, 0)
            ),
            widgetUpdateDispatcher = widgetUpdates
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as MealUiState.Success
        assertEquals(listOf("20260315", "20260315"), repository.requestedDates)
        assertEquals(listOf("api-key" to true), repository.syncCalls)
        assertEquals("20260315", state.meal?.date)
        assertEquals(1, widgetUpdates.count)
    }

    private fun createViewModel(
        repository: FakeMealDataRepository = FakeMealDataRepository(),
        settings: FakeAppSettings = FakeAppSettings(),
        clock: FakeAppClock = FakeAppClock(
            nowMillis = 1L,
            currentDate = LocalDate.of(2026, 3, 15),
            currentTime = LocalTime.of(9, 0)
        ),
        widgetUpdateDispatcher: RecordingWidgetUpdateDispatcher = RecordingWidgetUpdateDispatcher()
    ): MainViewModel = MainViewModel(
        repository = repository,
        preferences = settings,
        widgetUpdateDispatcher = widgetUpdateDispatcher,
        clock = clock
    )

    private fun meal(date: String) = MealEntity(
        date = date,
        breakfast = "조식",
        lunch = "중식",
        dinner = "석식",
        adspcfd = "",
        sumCal = "900"
    )

    private class RecordingWidgetUpdateDispatcher : WidgetUpdateDispatcher {
        var count = 0

        override fun updateAll() {
            count += 1
        }
    }

    private class FakeMealDataRepository : MealDataRepository {
        val requestedDates = mutableListOf<String>()
        val syncCalls = mutableListOf<Pair<String, Boolean>>()
        val mealByDate = linkedMapOf<String, MealEntity?>()

        var getMealHandler: suspend (String) -> Result<MealEntity?> = { date ->
            requestedDates += date
            Result.success(mealByDate[date])
        }

        var syncHandler: suspend (String, Boolean) -> Result<Unit> = { apiKey, forceReset ->
            syncCalls += apiKey to forceReset
            Result.success(Unit)
        }

        override suspend fun getMeal(date: String): Result<MealEntity?> = getMealHandler(date)

        override suspend fun syncIfNeeded(apiKey: String, forceReset: Boolean): Result<Unit> =
            syncHandler(apiKey, forceReset)
    }
}

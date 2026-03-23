package com.sleepysoong.armydiet.domain

import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.ExternalMealApi
import com.sleepysoong.armydiet.data.remote.ExternalMenu
import com.sleepysoong.armydiet.data.remote.ExternalResponse
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.data.remote.MndResponse
import com.sleepysoong.armydiet.data.remote.MndRow
import com.sleepysoong.armydiet.data.remote.MndService
import com.sleepysoong.armydiet.testutil.FakeAppClock
import com.sleepysoong.armydiet.testutil.FakeAppSettings
import com.sleepysoong.armydiet.testutil.FakeMealDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MealRepositoryTest {
    @Test
    fun syncIfNeeded_localSourceMergesRowsNormalizesDatesAndUpdatesSyncStatus() = runTest {
        val clock = FakeAppClock(
            nowMillis = 1_000L,
            currentDate = LocalDate.of(2026, 3, 15),
            currentTime = LocalTime.of(9, 0)
        )
        val settings = FakeAppSettings(
            apiKey = "api-key",
            mealSource = AppPreferences.SOURCE_LOCAL,
            mndUnitCode = "9999"
        )
        val mealDao = FakeMealDao(
            initialMeals = listOf(
                meal(date = "20260315", breakfast = "김치")
            )
        )
        val api = FakeMndApi().apply {
            responder = { serviceId, startIndex, endIndex ->
                when {
                    startIndex == 1 && endIndex == 1 -> mndResponse(serviceId, 2, emptyList())
                    startIndex == 1 && endIndex == 2 -> mndResponse(
                        serviceId,
                        2,
                        listOf(
                            MndRow(
                                dates = "2026-03-15(일)",
                                brst = "밥(1.2), 국",
                                lunc = null,
                                dinr = "닭볶음탕(5.6)",
                                adspcfd = "특식(9.1)",
                                sumCal = "900"
                            ),
                            MndRow(
                                dates = "2026.03.15",
                                brst = "밥(1.2)",
                                lunc = "돈가스(1.2)",
                                dinr = null,
                                adspcfd = null,
                                sumCal = null
                            )
                        )
                    )
                    else -> error("Unexpected page request: $startIndex..$endIndex")
                }
            }
        }

        val repository = MealRepository(mealDao, api, settings, { error("unused") }, clock)

        repository.syncIfNeeded("api-key", forceReset = false)
        val meal = repository.getMeal("20260315").getOrThrow()!!

        assertEquals(listOf("DS_TB_MNDT_DATEBYMLSVC_9999"), api.serviceIds.distinct())
        assertEquals(setOf("김치", "국", "밥"), meal.breakfast.toItemSet())
        assertEquals(setOf("돈가스"), meal.lunch.toItemSet())
        assertEquals(setOf("닭볶음탕"), meal.dinner.toItemSet())
        assertEquals(setOf("특식"), meal.adspcfd.toItemSet())
        assertEquals("900", meal.sumCal)
        assertEquals(2, settings.lastCheckedIndex.first())
        assertEquals(1_000L, settings.lastCheckedTimestamp.first())
    }

    @Test
    fun getMeal_returnsPlaceholderForBlankSectionsWithoutTriggeringSync() = runTest {
        val clock = FakeAppClock(
            nowMillis = 5_000L,
            currentDate = LocalDate.of(2026, 3, 16),
            currentTime = LocalTime.of(10, 0)
        )
        val settings = FakeAppSettings(
            apiKey = "api-key",
            mealSource = AppPreferences.SOURCE_LOCAL,
            lastCheckedTimestamp = 5_000L
        )
        val mealDao = FakeMealDao(
            initialMeals = listOf(
                meal(date = "20260316")
            )
        )
        val api = FakeMndApi()

        val repository = MealRepository(mealDao, api, settings, { error("unused") }, clock)

        val meal = repository.getMeal("20260316").getOrThrow()!!

        assertEquals("메뉴 정보 없음", meal.breakfast)
        assertEquals("메뉴 정보 없음", meal.lunch)
        assertEquals("메뉴 정보 없음", meal.dinner)
        assertEquals("메뉴 정보 없음", meal.adspcfd)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun syncIfNeeded_externalSourceUsesEndpointFactoryAndStoresCleanedMeal() = runTest {
        val clock = FakeAppClock(
            nowMillis = 8_000L,
            currentDate = LocalDate.of(2026, 3, 17),
            currentTime = LocalTime.of(7, 0)
        )
        val settings = FakeAppSettings(
            mealSource = AppPreferences.SOURCE_EXTERNAL,
            externalApiEndpoint = "https://example.com"
        )
        val mealDao = FakeMealDao()
        var capturedBaseUrl: String? = null
        val externalApi = FakeExternalMealApi(
            ExternalResponse(
                success = true,
                data = ExternalMenu(
                    date = "20260317",
                    breakfast = listOf("계란(1.2)", "빵"),
                    lunch = listOf("카레(5.6)"),
                    dinner = emptyList(),
                    total_calories = "1200"
                ),
                error = null
            )
        )

        val repository = MealRepository(
            mealDao = mealDao,
            api = FakeMndApi(),
            preferences = settings,
            externalApiFactory = { baseUrl ->
                capturedBaseUrl = baseUrl
                externalApi
            },
            clock = clock
        )

        repository.syncIfNeeded("ignored", forceReset = false)
        val meal = repository.getMeal("20260317").getOrThrow()!!

        assertEquals("https://example.com", capturedBaseUrl)
        assertEquals("20260317", externalApi.requestedDates.single())
        assertEquals("계란, 빵", meal.breakfast)
        assertEquals("카레", meal.lunch)
        assertEquals("메뉴 정보 없음", meal.dinner)
        assertEquals("1200", meal.sumCal)
        assertEquals(8_000L, settings.lastCheckedTimestamp.first())
    }

    private fun meal(
        date: String,
        breakfast: String = "",
        lunch: String = "",
        dinner: String = "",
        adspcfd: String = "",
        sumCal: String = ""
    ) = MealEntity(date, breakfast, lunch, dinner, adspcfd, sumCal)

    private fun mndResponse(serviceId: String, totalCount: Int, rows: List<MndRow>): MndResponse =
        mapOf(serviceId to MndService(totalCount, rows))

    private fun String.toItemSet(): Set<String> =
        split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    private class FakeMndApi : MndApi {
        val calls = mutableListOf<Triple<Int, Int, String>>()
        val serviceIds: List<String>
            get() = calls.map { it.third }
        var responder: (String, Int, Int) -> MndResponse = { _, _, _ -> emptyMap() }

        override suspend fun getMeals(
            apiKey: String,
            serviceId: String,
            startIndex: Int,
            endIndex: Int
        ): MndResponse {
            calls += Triple(startIndex, endIndex, serviceId)
            return responder(serviceId, startIndex, endIndex)
        }
    }

    private class FakeExternalMealApi(
        private val response: ExternalResponse<ExternalMenu>
    ) : ExternalMealApi {
        val requestedDates = mutableListOf<String>()

        override suspend fun getToday(): ExternalResponse<ExternalMenu> = response

        override suspend fun getMenu(date: String, meal: String?): ExternalResponse<ExternalMenu> {
            requestedDates += date
            return response
        }
    }
}

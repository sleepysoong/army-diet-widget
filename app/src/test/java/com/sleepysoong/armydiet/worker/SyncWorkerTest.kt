package com.sleepysoong.armydiet.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.sleepysoong.armydiet.data.local.AppSettings
import com.sleepysoong.armydiet.domain.MealDataRepository
import com.sleepysoong.armydiet.testutil.FakeAppSettings
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {
    private lateinit var context: Context
    private val defaultFactory = SyncWorker.dependenciesFactory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SyncWorker.dependenciesFactory = defaultFactory
    }

    @After
    fun tearDown() {
        SyncWorker.dependenciesFactory = defaultFactory
    }

    @Test
    fun doWork_withoutApiKey_returnsSuccessAndSkipsSync() = runTest {
        val repository = RecordingMealRepository()
        val widgetDispatcher = RecordingWidgetUpdateDispatcher()
        installDependencies(
            settings = FakeAppSettings(apiKey = null),
            repository = repository,
            widgetDispatcher = widgetDispatcher
        )

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(repository.syncCalls.isEmpty())
        assertEquals(0, widgetDispatcher.count)
    }

    @Test
    fun doWork_onSuccessfulSync_updatesWidgetsAndReturnsSuccess() = runTest {
        val repository = RecordingMealRepository()
        val widgetDispatcher = RecordingWidgetUpdateDispatcher()
        installDependencies(
            settings = FakeAppSettings(apiKey = "api-key"),
            repository = repository,
            widgetDispatcher = widgetDispatcher
        )

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(listOf("api-key" to false), repository.syncCalls)
        assertEquals(1, widgetDispatcher.count)
    }

    @Test
    fun doWork_http4xx_returnsFailure() = runTest {
        val repository = RecordingMealRepository().apply {
            syncResult = Result.failure(httpException(404))
        }
        installDependencies(FakeAppSettings(apiKey = "api-key"), repository, RecordingWidgetUpdateDispatcher())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun doWork_http5xx_returnsRetry() = runTest {
        val repository = RecordingMealRepository().apply {
            syncResult = Result.failure(httpException(500))
        }
        installDependencies(FakeAppSettings(apiKey = "api-key"), repository, RecordingWidgetUpdateDispatcher())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun doWork_ioException_returnsRetry() = runTest {
        val repository = RecordingMealRepository().apply {
            syncResult = Result.failure(IOException("network"))
        }
        installDependencies(FakeAppSettings(apiKey = "api-key"), repository, RecordingWidgetUpdateDispatcher())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun doWork_genericException_retriesBeforeThirdAttemptAndFailsAfter() = runTest {
        val firstRepository = RecordingMealRepository().apply {
            syncResult = Result.failure(IllegalStateException("boom"))
        }
        installDependencies(FakeAppSettings(apiKey = "api-key"), firstRepository, RecordingWidgetUpdateDispatcher())
        val retryResult = buildWorker(runAttemptCount = 2).doWork()

        val secondRepository = RecordingMealRepository().apply {
            syncResult = Result.failure(IllegalStateException("boom"))
        }
        installDependencies(FakeAppSettings(apiKey = "api-key"), secondRepository, RecordingWidgetUpdateDispatcher())
        val failureResult = buildWorker(runAttemptCount = 3).doWork()

        assertTrue(retryResult is ListenableWorker.Result.Retry)
        assertTrue(failureResult is ListenableWorker.Result.Failure)
    }

    private fun installDependencies(
        settings: AppSettings,
        repository: MealDataRepository,
        widgetDispatcher: WidgetUpdateDispatcher
    ) {
        SyncWorker.dependenciesFactory = {
            object : SyncWorkerDependencies {
                override val settings = settings
                override val repository = repository
                override val widgetUpdateDispatcher = widgetDispatcher
            }
        }
    }

    private fun buildWorker(runAttemptCount: Int = 0): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(runAttemptCount)
            .build()

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<String>(
            code,
            "{}".toResponseBody("application/json".toMediaType())
        )
    )

    private class RecordingMealRepository : MealDataRepository {
        val syncCalls = mutableListOf<Pair<String, Boolean>>()
        var syncResult: Result<Unit> = Result.success(Unit)

        override suspend fun getMeal(date: String) = Result.success(null)

        override suspend fun syncIfNeeded(apiKey: String, forceReset: Boolean): Result<Unit> {
            syncCalls += apiKey to forceReset
            return syncResult
        }
    }

    private class RecordingWidgetUpdateDispatcher : WidgetUpdateDispatcher {
        var count = 0

        override fun updateAll() {
            count += 1
        }
    }
}

package com.sleepysoong.armydiet.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sleepysoong.armydiet.di.AppContainer
import com.sleepysoong.armydiet.widget.ContextWidgetUpdateDispatcher
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "meal_sync_worker"
        private const val TAG = "SyncWorker"

        internal var dependenciesFactory: (Context) -> SyncWorkerDependencies = { context ->
            val container = AppContainer.getInstance(context)
            object : SyncWorkerDependencies {
                override val settings = container.preferences
                override val repository = container.mealRepository
                override val widgetUpdateDispatcher = ContextWidgetUpdateDispatcher(context)
            }
        }
    }

    override suspend fun doWork(): Result {
        val dependencies = dependenciesFactory(applicationContext)
        val preferences = dependencies.settings
        val repository = dependencies.repository

        return try {
            val apiKey = preferences.apiKey.first()
            
            if (apiKey.isNullOrBlank()) {
                Log.w(TAG, "API Key missing, skipping sync")
                return Result.success()
            }

            repository.syncIfNeeded(apiKey, forceReset = false)
                .onSuccess {
                    Log.i(TAG, "Sync completed successfully")
                    dependencies.widgetUpdateDispatcher.updateAll()
                }
                .onFailure { e ->
                    Log.e(TAG, "Sync failed", e)
                    throw e
                }

            Result.success()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()}", e)
            if (e.code() in 400..499) Result.failure() else Result.retry()
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

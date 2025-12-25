package com.sleepysoong.armydiet.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.remote.NetworkModule
import com.sleepysoong.armydiet.domain.MealRepository
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MealRepository(database.mealDao(), NetworkModule.api)
        val preferences = AppPreferences(applicationContext)

        return try {
            val apiKey = preferences.apiKey.first()
            if (apiKey.isNullOrBlank()) {
                Log.e("SyncWorker", "API Key is missing. Skipping sync.")
                return Result.failure()
            }

            repository.syncRecentData(apiKey)
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
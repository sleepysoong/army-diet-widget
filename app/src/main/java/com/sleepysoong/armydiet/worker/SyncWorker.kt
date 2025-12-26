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
import retrofit2.HttpException
import java.io.IOException

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val preferences = AppPreferences(applicationContext)
        val repository = MealRepository(database.mealDao(), NetworkModule.api, preferences)

        return try {
            val apiKey = preferences.apiKey.first()
            if (apiKey.isNullOrBlank()) {
                Log.e("SyncWorker", "API Key is missing. Skipping sync.")
                return Result.failure()
            }

            // 백그라운드 작업은 이어서 받기 (reset=false)
            repository.load(apiKey, reset = false)
            Result.success()
        } catch (e: HttpException) {
            Log.e("SyncWorker", "HTTP Error during sync", e)
            if (e.code() in 400..499) {
                 Result.failure()
            } else {
                 Result.retry()
            }
        } catch (e: IOException) {
            Log.e("SyncWorker", "Network Error during sync", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Unknown Error during sync", e)
            Result.failure()
        }
    }
}
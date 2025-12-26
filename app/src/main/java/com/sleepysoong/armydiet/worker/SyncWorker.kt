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
        val repository = MealRepository(database.mealDao(), NetworkModule.api)
        val preferences = AppPreferences(applicationContext)

        return try {
            val apiKey = preferences.apiKey.first()
            if (apiKey.isNullOrBlank()) {
                Log.e("SyncWorker", "API Key is missing. Skipping sync.")
                return Result.failure() // 키가 없으면 재시도하지 않음
            }

            repository.syncRecentData(apiKey)
            Result.success()
        } catch (e: HttpException) {
            // 4xx 에러 (Client Error)는 재시도해도 소용없으므로 failure 처리 고려
            // 하지만 일시적인 429(Too Many Requests)일 수도 있으니 신중해야 함
            Log.e("SyncWorker", "HTTP Error during sync", e)
            if (e.code() in 400..499) {
                 Result.failure()
            } else {
                 Result.retry()
            }
        } catch (e: IOException) {
            // 네트워크 오류 등 일시적 오류는 재시도
            Log.e("SyncWorker", "Network Error during sync", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Unknown Error during sync", e)
            Result.failure()
        }
    }
}

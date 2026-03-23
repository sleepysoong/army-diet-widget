package com.sleepysoong.armydiet.domain

import com.sleepysoong.armydiet.data.local.MealEntity

interface MealDataRepository {
    suspend fun getMeal(date: String): Result<MealEntity?>

    suspend fun syncIfNeeded(apiKey: String, forceReset: Boolean): Result<Unit>
}

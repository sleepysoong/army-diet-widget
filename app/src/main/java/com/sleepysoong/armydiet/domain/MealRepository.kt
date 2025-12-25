package com.sleepysoong.armydiet.domain

import android.util.Log
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealRepository(private val mealDao: MealDao, private val api: MndApi) {

    private val allergyRegex = Regex("\\(\\d+\\)")
    private val dateParseRegex = Regex("\\(.*?\\)")

    // API Key가 필요하면 syncRecentData를 호출해야 하므로, 여기서 처리하기보다
    // ViewModel이나 Worker에서 키를 넘겨받는 구조가 좋습니다.
    // 하지만 getMeal에서 자동 동기화를 하려면 키가 필요합니다.
    // 구조상 getMeal 호출 시 키를 넘겨주거나, 동기화를 분리해야 합니다.
    // 여기서는 getMeal에서 동기화 로직을 분리하고, 명시적으로 sync를 호출하도록 변경하거나
    // 키를 인자로 받겠습니다.

    suspend fun getMeal(date: String): Result<MealEntity?> {
        return withContext(Dispatchers.IO) {
            try {
                val meal = mealDao.getMeal(date)
                Result.success(meal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun syncRecentData(apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 총 개수 확인
                val countResponse = api.getMeals(apiKey, 1, 1)
                val totalCount = countResponse.service?.listTotalCount ?: 0

                if (totalCount == 0) return@withContext

                // 2. 마지막 30개 가져오기
                val end = totalCount
                val start = (totalCount - 30).coerceAtLeast(1)

                Log.d("MealRepository", "Fetching from $start to $end")
                val response = api.getMeals(apiKey, start, end)
                
                response.service?.rows?.let { rows ->
                    val entities = rows.mapNotNull { row ->
                        processRow(row)
                    }
                    
                    entities.forEach { entity ->
                        mealDao.insertMeal(entity)
                    }
                }
            } catch (e: Exception) {
                Log.e("MealRepository", "Sync failed", e)
                throw e
            }
        }
    }

    private fun processRow(row: com.sleepysoong.armydiet.data.remote.MndRow): MealEntity? {
        val dateStr = row.dates ?: return null
        val cleanDate = parseDate(dateStr) ?: return null

        return MealEntity(
            date = cleanDate,
            breakfast = cleanText(row.brst),
            lunch = cleanText(row.lunc),
            dinner = cleanText(row.dinr)
        )
    }

    private fun parseDate(dateStr: String): String? {
        return try {
            val raw = dateParseRegex.replace(dateStr, "").trim()
            val clean = raw.replace("-", "").replace(".", "")
            if (clean.length == 8) clean else null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanText(text: String?): String {
        if (text.isNullOrBlank()) return "메뉴 정보 없음"
        return allergyRegex.replace(text, "").trim()
    }
}
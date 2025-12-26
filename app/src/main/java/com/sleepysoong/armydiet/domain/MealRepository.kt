package com.sleepysoong.armydiet.domain

import android.util.Log
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealRepository(private val mealDao: MealDao, private val api: MndApi) {

    // (1), (1.2), (1.2.3) 등 숫자와 점으로 구성된 알레르기 정보 제거
    private val allergyRegex = Regex("\\([0-9.]+\\)")
    private val dateParseRegex = Regex("\\(.*?\\)")

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
                    val entities = rows.mapNotNull {
                        processRow(it)
                    }
                    
                    // 3. DB 저장 (Bulk Insert)
                    if (entities.isNotEmpty()) {
                        mealDao.insertMeals(entities)
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

package com.sleepysoong.armydiet.domain

import android.util.Log
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealRepository(private val mealDao: MealDao, private val api: MndApi) {

    private val allergyRegex = Regex("\\([0-9.]+\\)")
    private val dateParseRegex = Regex("\\(.*?\\)")

    suspend fun getMeal(date: String): Result<MealEntity?> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Getting meal for $date")
                val meal = mealDao.getMeal(date)
                if (meal != null) DebugLogger.log("Repo", "Cache hit")
                else DebugLogger.log("Repo", "Cache miss")
                Result.success(meal)
            } catch (e: Exception) {
                DebugLogger.log("Repo", "Get Error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun syncRecentData(apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Sync start. Key length: ${apiKey.length}")
                
                // 1. 총 개수 확인
                DebugLogger.log("Repo", "Fetching count...")
                val countResponse = api.getMeals(apiKey, 1, 1)
                
                // 에러 응답 체크
                if (countResponse.result != null) {
                     DebugLogger.log("Repo", "API Result: ${countResponse.result.code} - ${countResponse.result.message}")
                }
                
                val totalCount = countResponse.service?.listTotalCount ?: 0
                DebugLogger.log("Repo", "Total count: $totalCount")

                if (totalCount == 0) {
                    DebugLogger.log("Repo", "No data found (count=0)")
                    return@withContext
                }

                // 2. 마지막 30개 가져오기
                val end = totalCount
                val start = (totalCount - 30).coerceAtLeast(1)

                DebugLogger.log("Repo", "Fetching range $start-$end")
                val response = api.getMeals(apiKey, start, end)
                
                response.service?.rows?.let { rows ->
                    DebugLogger.log("Repo", "Rows received: ${rows.size}")
                    val entities = rows.mapNotNull { row ->
                        processRow(row)
                    }
                    
                    if (entities.isNotEmpty()) {
                        mealDao.insertMeals(entities)
                        DebugLogger.log("Repo", "Inserted ${entities.size} meals")
                    } else {
                        DebugLogger.log("Repo", "No valid entities to insert")
                    }
                } ?: run {
                    DebugLogger.log("Repo", "Rows is null")
                }
            } catch (e: Exception) {
                DebugLogger.log("Repo", "Sync Exception: ${e.javaClass.simpleName} - ${e.message}")
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
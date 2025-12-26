package com.sleepysoong.armydiet.domain

import android.util.Log
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.data.remote.MndRow
import com.sleepysoong.armydiet.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MealRepository(
    private val mealDao: MealDao,
    private val api: MndApi,
    private val preferences: AppPreferences
) {

    private val allergyRegex = Regex("\\([0-9.]+\\)")
    private val dateParseRegex = Regex("\\(.*?\\)")

    // 24시간 (ms)
    private val SYNC_INTERVAL = 24 * 60 * 60 * 1000L 

    suspend fun getMeal(date: String): Result<MealEntity?> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 자동 동기화 체크
                checkAndLoad()

                DebugLogger.log("Repo", "Getting meal for $date")
                val meal = mealDao.getMeal(date)
                
                if (meal != null) {
                    val cleanMeal = meal.copy(
                        breakfast = cleanText(meal.breakfast),
                        lunch = cleanText(meal.lunch),
                        dinner = cleanText(meal.dinner),
                        adspcfd = cleanText(meal.adspcfd)
                    )
                    Result.success(cleanMeal)
                } else {
                    // 데이터가 없으면 강제 로드 시도 (사용자 경험 향상)
                    // 하지만 무한 루프 방지를 위해 여기서 직접 호출하진 않고 null 반환
                    // ViewModel에서 "데이터 없음" -> "동기화 버튼" 유도
                    Result.success(null) 
                }
            } catch (e: Exception) {
                DebugLogger.log("Repo", "Get Error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private suspend fun checkAndLoad() {
        val lastTs = preferences.lastCheckedTimestamp.first()
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTs > SYNC_INTERVAL) {
            DebugLogger.log("Repo", "Auto-sync triggered (Time elapsed)")
            // API Key가 있어야 로드 가능
            val apiKey = preferences.apiKey.first()
            if (!apiKey.isNullOrBlank()) {
                load(apiKey, reset = false)
            }
        }
    }

    suspend fun load(apiKey: String, reset: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Load start. Reset=$reset")

                // 1. 전체 개수 확인
                val countResponse = api.getMeals(apiKey, 1, 1)
                val totalCount = countResponse.service?.listTotalCount ?: 0
                DebugLogger.log("Repo", "Total API count: $totalCount")

                if (totalCount == 0) return@withContext

                // 2. 범위 설정
                var startIdx = 1
                val lastIdx = preferences.lastCheckedIndex.first()
                
                if (!reset && lastIdx > 0) {
                    startIdx = lastIdx + 1
                }

                // 이미 최신이면 종료
                if (startIdx > totalCount) {
                    DebugLogger.log("Repo", "Already up to date. (start $startIdx > total $totalCount)")
                    preferences.updateSyncStatus(totalCount, System.currentTimeMillis())
                    return@withContext
                }

                // 3. 배치 다운로드 (한 번에 너무 많이 받으면 메모리 터질 수 있으니 1000개씩 끊어서)
                val batchSize = 1000
                // 최대 10만개 혹은 지난번 이후부터 끝까지
                // 사용자가 "reset=false면 10000개까지만"이라고 했으나, 
                // 인덱스가 뒤죽박죽일 수 있으니 가능한 끝까지(TotalCount) 가는게 안전.
                // 다만 너무 많으면(예: 10만개) 오래 걸리니 제한을 둡니다.
                
                val limit = if (reset) 100000 else 10000 
                var currentStart = startIdx
                var processedCount = 0

                while (currentStart <= totalCount && processedCount < limit) {
                    val currentEnd = (currentStart + batchSize - 1).coerceAtMost(totalCount)
                    DebugLogger.log("Repo", "Fetching batch $currentStart-$currentEnd")
                    
                    val response = api.getMeals(apiKey, currentStart, currentEnd)
                    val rows = response.service?.rows ?: emptyList()
                    
                    if (rows.isNotEmpty()) {
                        val processedMap = processRows(rows)
                        upsertMeals(processedMap)
                        processedCount += rows.size
                    } else {
                        // 데이터가 비어있으면 루프 중단 (API 오류 등)
                        DebugLogger.log("Repo", "Empty batch, stopping")
                        break
                    }
                    
                    currentStart += batchSize
                }

                // 상태 업데이트
                // 완전히 끝까지 다 받았을 때만 totalCount로 업데이트
                // 제한(limit) 때문에 중간에 멈췄으면 currentStart - 1 저장
                val newLastIndex = (currentStart - 1).coerceAtMost(totalCount)
                preferences.updateSyncStatus(newLastIndex, System.currentTimeMillis())
                DebugLogger.log("Repo", "Load complete. New index: $newLastIndex")

            } catch (e: Exception) {
                DebugLogger.log("Repo", "Load failed: ${e.message}")
                throw e
            }
        }
    }

    private fun processRows(rows: List<MndRow>): Map<String, MealEntity> {
        val processed = HashMap<String, MealEntity>()

        for (row in rows) {
            val dateClean = parseDate(row.dates ?: "") ?: continue
            
            val existing = processed[dateClean] ?: MealEntity(
                date = dateClean,
                breakfast = "", lunch = "", dinner = "", adspcfd = "", sumCal = ""
            )

            processed[dateClean] = existing.copy(
                breakfast = appendString(existing.breakfast, row.brst),
                lunch = appendString(existing.lunch, row.lunc),
                dinner = appendString(existing.dinner, row.dinr),
                adspcfd = appendString(existing.adspcfd, row.adspcfd),
                sumCal = if (!row.sumCal.isNullOrBlank()) row.sumCal else existing.sumCal
            )
        }
        return processed
    }

    private fun appendString(current: String, new: String?): String {
        if (new.isNullOrBlank()) return current
        if (current.isEmpty()) return new
        return "$current, $new"
    }

    private suspend fun upsertMeals(newMeals: Map<String, MealEntity>) {
        val dates = newMeals.keys.toList()
        val existingEntities = mealDao.getMealsByDates(dates).associateBy { it.date }
        val toInsert = ArrayList<MealEntity>()

        for ((date, newMeal) in newMeals) {
            val existing = existingEntities[date]
            
            val merged = if (existing != null) {
                MealEntity(
                    date = date,
                    breakfast = mergeMenuItems(existing.breakfast, newMeal.breakfast),
                    lunch = mergeMenuItems(existing.lunch, newMeal.lunch),
                    dinner = mergeMenuItems(existing.dinner, newMeal.dinner),
                    adspcfd = mergeMenuItems(existing.adspcfd, newMeal.adspcfd),
                    sumCal = if (newMeal.sumCal.isNotBlank()) newMeal.sumCal else existing.sumCal
                )
            } else {
                MealEntity(
                    date = date,
                    breakfast = mergeMenuItems("", newMeal.breakfast),
                    lunch = mergeMenuItems("", newMeal.lunch),
                    dinner = mergeMenuItems("", newMeal.dinner),
                    adspcfd = mergeMenuItems("", newMeal.adspcfd),
                    sumCal = newMeal.sumCal
                )
            }
            toInsert.add(merged)
        }
        
        if (toInsert.isNotEmpty()) {
            mealDao.insertMeals(toInsert)
        }
    }

    private fun mergeMenuItems(oldStr: String, newStr: String): String {
        if (oldStr.isBlank() && newStr.isBlank()) return ""

        val items = HashSet<String>()
        
        if (oldStr.isNotBlank()) {
            oldStr.split(",").forEach { 
                val trimmed = it.trim()
                if (trimmed.isNotEmpty()) items.add(trimmed)
            }
        }
        
        if (newStr.isNotBlank()) {
            newStr.split(",").forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty()) items.add(trimmed)
            }
        }

        return items.sorted().joinToString(", ")
    }

    private fun parseDate(dateStr: String): String? {
        val dateRaw = dateStr.split("(")[0].trim()
        val formats = listOf("yyyy-MM-dd", "yyyy.MM.dd", "yyyyMMdd")
        
        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.KOREA)
                parser.isLenient = false
                val date = parser.parse(dateRaw)
                if (date != null) {
                    val output = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
                    return output.format(date)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    private fun cleanText(text: String): String {
        if (text.isBlank()) return "메뉴 정보 없음"
        return allergyRegex.replace(text, "").trim()
    }
}

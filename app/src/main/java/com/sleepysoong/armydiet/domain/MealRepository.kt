package com.sleepysoong.armydiet.domain

import android.util.Log
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.data.remote.MndRow
import com.sleepysoong.armydiet.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MealRepository(private val mealDao: MealDao, private val api: MndApi) {

    private val allergyRegex = Regex("\\([0-9.]+\\)")
    // Go: strings.Split(dateStr, "(")[0]
    
    // Go: DietService.GetMenu
    suspend fun getMeal(date: String): Result<MealEntity?> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Getting meal for $date")
                val meal = mealDao.getMeal(date)
                
                // 알레르기 정보 제거 후 반환 (Entity 자체는 원본 유지, 뷰용 데이터만 가공해도 되지만
                // Go 코드에서는 GetMenu 시점에 제거함)
                if (meal != null) {
                    val cleanMeal = meal.copy(
                        breakfast = cleanText(meal.breakfast),
                        lunch = cleanText(meal.lunch),
                        dinner = cleanText(meal.dinner),
                        adspcfd = cleanText(meal.adspcfd)
                    )
                    Result.success(cleanMeal)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                DebugLogger.log("Repo", "Get Error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // Go: DietService.SyncData
    suspend fun syncRecentData(apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Sync start")
                
                // 1. FetchTotalCount
                val countResponse = api.getMeals(apiKey, 1, 1)
                val totalCount = countResponse.service?.listTotalCount ?: 0
                DebugLogger.log("Repo", "Total count: $totalCount")

                if (totalCount == 0) return@withContext

                // Go와 달리 전체를 다 받으면 모바일에서 무리일 수 있으므로 
                // 최근 100개(약 1달치) 정도만 가져오도록 조정하거나, 
                // 사용자가 원하면 전체 동기화도 가능하게 해야 함.
                // 여기서는 "오늘 식단"이 주 목적이므로 최근 100건만 가져옵니다.
                // Go 코드는 lastIdx를 저장해서 증분 업데이트를 하지만, 
                // 앱에서는 단순화를 위해 매번 최근 데이터를 갱신합니다.
                
                val batchSize = 100
                val startIdx = (totalCount - batchSize + 1).coerceAtLeast(1)
                val endIdx = totalCount

                DebugLogger.log("Repo", "Fetching range $startIdx-$endIdx")
                val response = api.getMeals(apiKey, startIdx, endIdx)
                
                val rows = response.service?.rows ?: emptyList()
                if (rows.isEmpty()) return@withContext

                // 2. ProcessRows (메모리 상에서 병합)
                val processedMap = processRows(rows)
                
                // 3. UpsertMeals (DB와 병합 후 저장)
                upsertMeals(processedMap)
                
                DebugLogger.log("Repo", "Sync complete. Processed ${processedMap.size} dates.")

            } catch (e: Exception) {
                DebugLogger.log("Repo", "Sync failed: ${e.message}")
                throw e
            }
        }
    }

    // Go: DietService.processRows
    private fun processRows(rows: List<MndRow>): Map<String, MealEntity> {
        val processed = HashMap<String, MealEntity>()

        for (row in rows) {
            val dateClean = parseDate(row.dates ?: "") ?: continue
            
            val existing = processed[dateClean] ?: MealEntity(
                date = dateClean,
                breakfast = "", lunch = "", dinner = "", adspcfd = "", sumCal = ""
            )

            // Go: Merge logic inline in processRows
            processed[dateClean] = existing.copy(
                breakfast = mergeStrings(existing.breakfast, row.brst),
                lunch = mergeStrings(existing.lunch, row.lunc),
                dinner = mergeStrings(existing.dinner, row.dinr),
                adspcfd = mergeStrings(existing.adspcfd, row.adspcfd),
                sumCal = if (row.sumCal.isNullOrBlank()) existing.sumCal else row.sumCal
            )
        }
        return processed
    }

    // Go: mergeMenuItems (Helper) -> processRows에서 inline으로 호출됨
    // But processRows adds with ", ". mergeMenuItems does split/sort/join.
    // Go 코드의 processRows는 단순히 string concat을 하고 있고, 
    // models.Meal.Merge 함수가 mergeMenuItems를 호출함.
    // 여기서는 processRows 단계에서 API 데이터끼리의 병합을 수행하고, 
    // upsertMeals에서 DB 데이터와의 병합을 수행합니다.

    private fun mergeStrings(current: String, new: String?): String {
        if (new.isNullOrBlank()) return current
        if (current.isBlank()) return new
        return "$current, $new"
    }

    // Go: Repository.UpsertMeals & models.Meal.Merge
    private suspend fun upsertMeals(newMeals: Map<String, MealEntity>) {
        // 1. 기존 데이터 조회
        val dates = newMeals.keys.toList()
        val existingEntities = mealDao.getMealsByDates(dates).associateBy { it.date }
        
        val toInsert = ArrayList<MealEntity>()

        for ((date, newMeal) in newMeals) {
            val existing = existingEntities[date]
            
            val merged = if (existing != null) {
                // Go: models.Meal.Merge 로직 구현
                MealEntity(
                    date = date,
                    breakfast = mergeMenuItems(existing.breakfast, newMeal.breakfast),
                    lunch = mergeMenuItems(existing.lunch, newMeal.lunch),
                    dinner = mergeMenuItems(existing.dinner, newMeal.dinner),
                    adspcfd = mergeMenuItems(existing.adspcfd, newMeal.adspcfd),
                    sumCal = if (newMeal.sumCal.isNotBlank()) newMeal.sumCal else existing.sumCal
                )
            } else {
                // 신규 데이터도 내부적으로 중복 정리 필요 (processRows에서 단순 concat 했으므로)
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
        
        mealDao.insertMeals(toInsert)
    }

    // Go: mergeMenuItems
    private fun mergeMenuItems(oldStr: String, newStr: String): String {
        if (oldStr.isBlank()) return sortAndJoin(newStr) // 신규 데이터도 정렬 필요
        if (newStr.isBlank()) return sortAndJoin(oldStr)

        val combined = "$oldStr, $newStr"
        return sortAndJoin(combined)
    }

    private fun sortAndJoin(raw: String): String {
        if (raw.isBlank()) return ""
        
        val items = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet() // 중복 제거
        
        return items.sorted().joinToString(", ") // 정렬 후 병합
    }

    // Go: parseDate
    private fun parseDate(dateStr: String): String? {
        val dateRaw = dateStr.split("(")[0].trim()
        
        // Go formats: "2006-01-02", "2006.01.02", "20060102"
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

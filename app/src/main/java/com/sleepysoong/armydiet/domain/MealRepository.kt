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

    // (1), (1.2), (1.2.3) 등 숫자와 점으로 구성된 알레르기 정보 제거
    private val allergyRegex = Regex("\\([0-9.]+\\)")
    private val dateParseRegex = Regex("\\(.*?\\)")

    suspend fun getMeal(date: String): Result<MealEntity?> {
        return withContext(Dispatchers.IO) {
            try {
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
                    Result.success(null)
                }
            } catch (e: Exception) {
                DebugLogger.log("Repo", "Get Error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun syncRecentData(apiKey: String) {
        withContext(Dispatchers.IO) {
            try {
                DebugLogger.log("Repo", "Sync start")
                
                // 1. FetchTotalCount
                val countResponse = api.getMeals(apiKey, 1, 1)
                val totalCount = countResponse.service?.listTotalCount ?: 0
                DebugLogger.log("Repo", "Total count: $totalCount")

                if (totalCount == 0) return@withContext

                // Go 코드와 마찬가지로 인덱스 기반으로 가져옵니다.
                // 데이터가 날짜순 정렬이 아니더라도, 전체 데이터 중 일부를 갱신하는 개념입니다.
                // 여기서는 최근 100건을 가져옵니다.
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

            // Go 코드 로직을 정확히 따름:
            // "if row.Brst != "" { if m.Breakfast != "" { m.Breakfast += ", " }; m.Breakfast += row.Brst }"
            // 즉, 여기서는 단순 연결만 하고, 나중에 mergeMenuItems로 정렬/중복제거
            
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

    // Go: Repository.UpsertMeals & models.Meal.Merge
    private suspend fun upsertMeals(newMeals: Map<String, MealEntity>) {
        val dates = newMeals.keys.toList()
        
        // DB에서 기존 데이터 조회
        val existingEntities = mealDao.getMealsByDates(dates).associateBy { it.date }
        
        val toInsert = ArrayList<MealEntity>()

        for ((date, newMeal) in newMeals) {
            val existing = existingEntities[date]
            
            val merged = if (existing != null) {
                // 기존 데이터가 있으면 병합 (Go: models.Meal.Merge)
                MealEntity(
                    date = date,
                    breakfast = mergeMenuItems(existing.breakfast, newMeal.breakfast),
                    lunch = mergeMenuItems(existing.lunch, newMeal.lunch),
                    dinner = mergeMenuItems(existing.dinner, newMeal.dinner),
                    adspcfd = mergeMenuItems(existing.adspcfd, newMeal.adspcfd),
                    sumCal = if (newMeal.sumCal.isNotBlank()) newMeal.sumCal else existing.sumCal
                )
            } else {
                // 신규 데이터도 내부적으로 중복 정리 및 정렬 필요 (processRows에서 콤마로 단순 연결했으므로)
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

    // Go: mergeMenuItems (split -> trim -> dedup -> sort -> join)
    private fun mergeMenuItems(oldStr: String, newStr: String): String {
        // Go 코드:
        // if oldStr == "" { return newStr } -> 하지만 newStr이 정렬 안되어있을 수 있으므로 항상 정렬 로직 통과
        // if newStr == "" { return oldStr }
        
        // 둘 다 비어있으면 빈 문자열
        if (oldStr.isBlank() && newStr.isBlank()) return ""

        val items = HashSet<String>()
        
        // Old String 처리
        if (oldStr.isNotBlank()) {
            oldStr.split(",").forEach { 
                val trimmed = it.trim()
                if (trimmed.isNotEmpty()) items.add(trimmed)
            }
        }
        
        // New String 처리
        if (newStr.isNotBlank()) {
            newStr.split(",").forEach {
                val trimmed = it.trim()
                if (trimmed.isNotEmpty()) items.add(trimmed)
            }
        }

        // 정렬 및 조립
        return items.sorted().joinToString(", ")
    }

    // Go: parseDate
    private fun parseDate(dateStr: String): String? {
        // Go: strings.Split(dateStr, "(")[0]
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
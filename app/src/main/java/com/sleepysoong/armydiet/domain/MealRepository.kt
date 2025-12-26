package com.sleepysoong.armydiet.domain

import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.data.remote.MndRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MealRepository(
    private val mealDao: MealDao,
    private val api: MndApi,
    private val preferences: AppPreferences
) {
    companion object {
        private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val BATCH_SIZE = 1000
        private const val MAX_ITEMS_INCREMENTAL = 10_000
        private const val MAX_ITEMS_FULL = 100_000
        
        private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")
        private val DATE_FORMATS = listOf("yyyy-MM-dd", "yyyy.MM.dd", "yyyyMMdd")
    }
    
    private val syncMutex = Mutex()
    
    suspend fun getMeal(date: String): Result<MealEntity?> = withContext(Dispatchers.IO) {
        runCatching {
            checkAndSyncIfNeeded()
            mealDao.getMeal(date)?.cleaned()
        }
    }
    
    suspend fun syncIfNeeded(apiKey: String, forceReset: Boolean = false): Result<Unit> = 
        withContext(Dispatchers.IO) {
            runCatching {
                syncMutex.withLock {
                    performSync(apiKey, forceReset)
                }
            }
        }
    
    private suspend fun checkAndSyncIfNeeded() {
        val lastSync = preferences.lastCheckedTimestamp.first()
        val now = System.currentTimeMillis()
        
        if (now - lastSync > SYNC_INTERVAL_MS) {
            val apiKey = preferences.apiKey.first()
            if (!apiKey.isNullOrBlank() && syncMutex.tryLock()) {
                try {
                    performSync(apiKey, reset = false)
                } finally {
                    syncMutex.unlock()
                }
            }
        }
    }
    
    private suspend fun performSync(apiKey: String, reset: Boolean) {
        val countResponse = api.getMeals(apiKey, 1, 1)
        val totalCount = countResponse.service?.listTotalCount ?: return
        
        if (totalCount == 0) return
        
        val lastIndex = if (reset) 0 else preferences.lastCheckedIndex.first()
        var startIdx = lastIndex + 1
        
        if (startIdx > totalCount) {
            preferences.updateSyncStatus(totalCount, System.currentTimeMillis())
            return
        }
        
        val maxItems = if (reset) MAX_ITEMS_FULL else MAX_ITEMS_INCREMENTAL
        var processedCount = 0
        
        while (startIdx <= totalCount && processedCount < maxItems) {
            val endIdx = (startIdx + BATCH_SIZE - 1).coerceAtMost(totalCount)
            val response = api.getMeals(apiKey, startIdx, endIdx)
            val rows = response.service?.rows
            
            if (rows.isNullOrEmpty()) break
            
            processBatch(rows)
            processedCount += rows.size
            startIdx += BATCH_SIZE
        }
        
        val newLastIndex = (startIdx - 1).coerceAtMost(totalCount)
        preferences.updateSyncStatus(newLastIndex, System.currentTimeMillis())
    }
    
    private suspend fun processBatch(rows: List<MndRow>) {
        val mealsByDate = rows.groupBy { parseDate(it.dates) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (date, dateRows) ->
                dateRows.fold(MealEntity.empty(date)) { acc, row ->
                    acc.merge(row)
                }
            }
        
        if (mealsByDate.isEmpty()) return
        
        val existingMeals = mealDao.getMealsByDates(mealsByDate.keys.toList())
            .associateBy { it.date }
        
        val toInsert = mealsByDate.map { (date, newMeal) ->
            existingMeals[date]?.merge(newMeal) ?: newMeal
        }
        
        mealDao.insertMeals(toInsert)
    }
    
    private fun parseDate(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        val rawDate = dateStr.substringBefore("(").trim()
        
        for (format in DATE_FORMATS) {
            runCatching {
                val parser = SimpleDateFormat(format, Locale.KOREA).apply { isLenient = false }
                val date = parser.parse(rawDate) ?: return@runCatching null
                return SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(date)
            }
        }
        return null
    }
    
    private fun MealEntity.cleaned(): MealEntity = copy(
        breakfast = cleanText(breakfast),
        lunch = cleanText(lunch),
        dinner = cleanText(dinner),
        adspcfd = cleanText(adspcfd)
    )
    
    private fun cleanText(text: String): String {
        if (text.isBlank()) return "메뉴 정보 없음"
        return ALLERGY_REGEX.replace(text, "").trim()
    }
}

private fun MealEntity.Companion.empty(date: String) = MealEntity(
    date = date,
    breakfast = "",
    lunch = "",
    dinner = "",
    adspcfd = "",
    sumCal = ""
)

private fun MealEntity.merge(row: MndRow): MealEntity = copy(
    breakfast = appendIfNotBlank(breakfast, row.brst),
    lunch = appendIfNotBlank(lunch, row.lunc),
    dinner = appendIfNotBlank(dinner, row.dinr),
    adspcfd = appendIfNotBlank(adspcfd, row.adspcfd),
    sumCal = row.sumCal?.takeIf { it.isNotBlank() } ?: sumCal
)

private fun MealEntity.merge(other: MealEntity): MealEntity = copy(
    breakfast = mergeMenuItems(breakfast, other.breakfast),
    lunch = mergeMenuItems(lunch, other.lunch),
    dinner = mergeMenuItems(dinner, other.dinner),
    adspcfd = mergeMenuItems(adspcfd, other.adspcfd),
    sumCal = other.sumCal.takeIf { it.isNotBlank() } ?: sumCal
)

private fun appendIfNotBlank(current: String, new: String?): String {
    if (new.isNullOrBlank()) return current
    return if (current.isEmpty()) new else "$current, $new"
}

private fun mergeMenuItems(a: String, b: String): String {
    if (a.isBlank() && b.isBlank()) return ""
    val items = (a.split(",") + b.split(","))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSortedSet()
    return items.joinToString(", ")
}

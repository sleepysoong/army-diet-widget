package com.sleepysoong.armydiet.data.local

import kotlinx.coroutines.flow.Flow

interface AppSettings {
    val apiKey: Flow<String?>
    val lastCheckedIndex: Flow<Int>
    val lastCheckedTimestamp: Flow<Long>
    val highlightKeywords: Flow<Set<String>>
    val mealSource: Flow<String>
    val externalApiEndpoint: Flow<String?>
    val mndUnitCode: Flow<String>

    suspend fun saveApiKey(key: String)
    suspend fun clearApiKey()
    suspend fun updateSyncStatus(index: Int, timestamp: Long)
    suspend fun setMealSource(source: String)
    suspend fun setExternalApiEndpoint(endpoint: String)
    suspend fun setMndUnitCode(code: String)
    suspend fun clearExternalApiEndpoint()
    suspend fun addKeyword(keyword: String)
    suspend fun removeKeyword(keyword: String)
}

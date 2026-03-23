package com.sleepysoong.armydiet.testutil

import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppSettings(
    apiKey: String? = null,
    lastCheckedIndex: Int = 0,
    lastCheckedTimestamp: Long = 0L,
    highlightKeywords: Set<String> = AppPreferences.DEFAULT_KEYWORDS,
    mealSource: String = AppPreferences.SOURCE_LOCAL,
    externalApiEndpoint: String? = null,
    mndUnitCode: String = AppPreferences.DEFAULT_MND_UNIT_CODE
) : AppSettings {
    private val apiKeyState = MutableStateFlow(apiKey)
    private val lastCheckedIndexState = MutableStateFlow(lastCheckedIndex)
    private val lastCheckedTimestampState = MutableStateFlow(lastCheckedTimestamp)
    private val highlightKeywordsState = MutableStateFlow(highlightKeywords)
    private val mealSourceState = MutableStateFlow(mealSource)
    private val externalApiEndpointState = MutableStateFlow(externalApiEndpoint)
    private val mndUnitCodeState = MutableStateFlow(mndUnitCode)

    override val apiKey: Flow<String?> = apiKeyState
    override val lastCheckedIndex: Flow<Int> = lastCheckedIndexState
    override val lastCheckedTimestamp: Flow<Long> = lastCheckedTimestampState
    override val highlightKeywords: Flow<Set<String>> = highlightKeywordsState
    override val mealSource: Flow<String> = mealSourceState
    override val externalApiEndpoint: Flow<String?> = externalApiEndpointState
    override val mndUnitCode: Flow<String> = mndUnitCodeState

    override suspend fun saveApiKey(key: String) {
        apiKeyState.value = key
    }

    override suspend fun clearApiKey() {
        apiKeyState.value = null
    }

    override suspend fun updateSyncStatus(index: Int, timestamp: Long) {
        lastCheckedIndexState.value = index
        lastCheckedTimestampState.value = timestamp
    }

    override suspend fun setMealSource(source: String) {
        mealSourceState.value = source
    }

    override suspend fun setExternalApiEndpoint(endpoint: String) {
        externalApiEndpointState.value = endpoint
    }

    override suspend fun setMndUnitCode(code: String) {
        mndUnitCodeState.value = code
    }

    override suspend fun clearExternalApiEndpoint() {
        externalApiEndpointState.value = null
    }

    override suspend fun addKeyword(keyword: String) {
        highlightKeywordsState.value = highlightKeywordsState.value + keyword.trim()
    }

    override suspend fun removeKeyword(keyword: String) {
        highlightKeywordsState.value = highlightKeywordsState.value - keyword.trim()
    }
}

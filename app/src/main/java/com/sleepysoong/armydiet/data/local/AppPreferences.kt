package com.sleepysoong.armydiet.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {
    private val API_KEY = stringPreferencesKey("api_key")
    private val LAST_CHECKED_INDEX = intPreferencesKey("last_checked_index")
    private val LAST_CHECKED_TIMESTAMP = longPreferencesKey("last_checked_timestamp")
    private val KEYWORD_LIST = stringPreferencesKey("highlight_keywords")
    private val MEAL_SOURCE = stringPreferencesKey("meal_source")
    private val EXTERNAL_API_ENDPOINT = stringPreferencesKey("external_api_endpoint")
    private val MND_UNIT_CODE = stringPreferencesKey("mnd_unit_code")

    companion object {
        val DEFAULT_KEYWORDS = setOf(
            "소시지", "소세지", "닭", "삼겹살", "불고기", "돈가스", "갈비", "돼지", "소고기", "고기"
        )
        const val DEFAULT_MND_UNIT_CODE = "7369"
        const val SOURCE_LOCAL = "local"
        const val SOURCE_EXTERNAL = "external"
    }

    val apiKey: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("AppPreferences", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[API_KEY] }

    val lastCheckedIndex: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_CHECKED_INDEX] ?: 0 }

    val lastCheckedTimestamp: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_CHECKED_TIMESTAMP] ?: 0L }

    val highlightKeywords: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val stored = preferences[KEYWORD_LIST]
            if (stored.isNullOrBlank()) {
                DEFAULT_KEYWORDS
            } else {
                stored.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val mealSource: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MEAL_SOURCE] ?: SOURCE_LOCAL }

    val externalApiEndpoint: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[EXTERNAL_API_ENDPOINT] }

    val mndUnitCode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MND_UNIT_CODE]?.takeIf { it.isNotBlank() } ?: DEFAULT_MND_UNIT_CODE }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }

    suspend fun updateSyncStatus(index: Int, timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CHECKED_INDEX] = index
            preferences[LAST_CHECKED_TIMESTAMP] = timestamp
        }
    }

    suspend fun setMealSource(source: String) {
        context.dataStore.edit { preferences ->
            preferences[MEAL_SOURCE] = source
        }
    }

    suspend fun setExternalApiEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[EXTERNAL_API_ENDPOINT] = endpoint
        }
    }

    suspend fun setMndUnitCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[MND_UNIT_CODE] = code.trim().ifBlank { DEFAULT_MND_UNIT_CODE }
        }
    }

    suspend fun clearExternalApiEndpoint() {
        context.dataStore.edit { preferences ->
            preferences.remove(EXTERNAL_API_ENDPOINT)
        }
    }

    suspend fun addKeyword(keyword: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEYWORD_LIST]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet()
                ?: DEFAULT_KEYWORDS.toMutableSet()
            current.add(keyword.trim())
            preferences[KEYWORD_LIST] = current.joinToString(",")
        }
    }

    suspend fun removeKeyword(keyword: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEYWORD_LIST]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet()
                ?: DEFAULT_KEYWORDS.toMutableSet()
            current.remove(keyword.trim())
            preferences[KEYWORD_LIST] = current.joinToString(",")
        }
    }
}

package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_config")

class WidgetConfig(private val context: Context, private val appWidgetId: Int) {
    
    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_SHOW_CALORIES = 1
        const val DEFAULT_TAG_SCALE = 1.3f
        const val DEFAULT_HEADER_SCALE = 2.3f
        
        const val MIN_FONT_SCALE = 0.7f
        const val MAX_FONT_SCALE = 2.0f
        
        const val MIN_TAG_SCALE = 1.0f
        const val MAX_TAG_SCALE = 2.0f
        
        const val MIN_HEADER_SCALE = 1.5f
        const val MAX_HEADER_SCALE = 3.5f
        
        // 위젯별 키 생성 헬퍼
        private fun fontScaleKey(widgetId: Int) = floatPreferencesKey("font_scale_$widgetId")
        private fun showCaloriesKey(widgetId: Int) = intPreferencesKey("show_calories_$widgetId")
        private fun tagScaleKey(widgetId: Int) = floatPreferencesKey("tag_scale_$widgetId")
        private fun headerScaleKey(widgetId: Int) = floatPreferencesKey("header_scale_$widgetId")
        
        // 위젯 삭제 시 설정 정리
        suspend fun clearConfig(context: Context, appWidgetId: Int) {
            context.widgetDataStore.edit { prefs ->
                prefs.remove(fontScaleKey(appWidgetId))
                prefs.remove(showCaloriesKey(appWidgetId))
                prefs.remove(tagScaleKey(appWidgetId))
                prefs.remove(headerScaleKey(appWidgetId))
            }
        }
    }
    
    // 위젯별 키
    private val fontScaleKey = fontScaleKey(appWidgetId)
    private val showCaloriesKey = showCaloriesKey(appWidgetId)
    private val tagScaleKey = tagScaleKey(appWidgetId)
    private val headerScaleKey = headerScaleKey(appWidgetId)
    
    val fontScale: Flow<Float> = context.widgetDataStore.data
        .map { it[fontScaleKey] ?: DEFAULT_FONT_SCALE }
        
    val tagScale: Flow<Float> = context.widgetDataStore.data
        .map { it[tagScaleKey] ?: DEFAULT_TAG_SCALE }
        
    val headerScale: Flow<Float> = context.widgetDataStore.data
        .map { it[headerScaleKey] ?: DEFAULT_HEADER_SCALE }
    
    val showCalories: Flow<Boolean> = context.widgetDataStore.data
        .map { (it[showCaloriesKey] ?: DEFAULT_SHOW_CALORIES) == 1 }
    
    suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        context.widgetDataStore.edit { it[fontScaleKey] = clamped }
    }
    
    suspend fun setTagScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_TAG_SCALE, MAX_TAG_SCALE)
        context.widgetDataStore.edit { it[tagScaleKey] = clamped }
    }
    
    suspend fun setHeaderScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_HEADER_SCALE, MAX_HEADER_SCALE)
        context.widgetDataStore.edit { it[headerScaleKey] = clamped }
    }
    
    suspend fun setShowCalories(show: Boolean) {
        context.widgetDataStore.edit { it[showCaloriesKey] = if (show) 1 else 0 }
    }
}

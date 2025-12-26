package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_config")

/**
 * 위젯 설정 관리
 * - fontSize: 폰트 크기 배율 (0.8 ~ 1.5)
 * - backgroundAlpha: 배경 투명도 (0.0 ~ 1.0)
 * - showCalories: 칼로리 표시 여부
 */
class WidgetConfig(private val context: Context) {
    
    companion object {
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val BG_ALPHA = floatPreferencesKey("bg_alpha")
        private val SHOW_CALORIES = intPreferencesKey("show_calories")
        
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_BG_ALPHA = 1.0f
        const val DEFAULT_SHOW_CALORIES = 1
        
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 1.5f
    }
    
    val fontScale: Flow<Float> = context.widgetDataStore.data
        .map { it[FONT_SCALE] ?: DEFAULT_FONT_SCALE }
    
    val backgroundAlpha: Flow<Float> = context.widgetDataStore.data
        .map { it[BG_ALPHA] ?: DEFAULT_BG_ALPHA }
    
    val showCalories: Flow<Boolean> = context.widgetDataStore.data
        .map { (it[SHOW_CALORIES] ?: DEFAULT_SHOW_CALORIES) == 1 }
    
    suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        context.widgetDataStore.edit { it[FONT_SCALE] = clamped }
    }
    
    suspend fun setBackgroundAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0f, 1f)
        context.widgetDataStore.edit { it[BG_ALPHA] = clamped }
    }
    
    suspend fun setShowCalories(show: Boolean) {
        context.widgetDataStore.edit { it[SHOW_CALORIES] = if (show) 1 else 0 }
    }
}

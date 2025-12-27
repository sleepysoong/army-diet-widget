package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_config")

class WidgetConfig(private val context: Context) {
    
    companion object {
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        private val SHOW_CALORIES = intPreferencesKey("show_calories")
        
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_LETTER_SPACING = -0.15f // 기본 -15%
        const val DEFAULT_SHOW_CALORIES = 1
        
        const val MIN_FONT_SCALE = 0.7f
        const val MAX_FONT_SCALE = 1.5f
        const val MIN_LETTER_SPACING = -1.0f // -100%
        const val MAX_LETTER_SPACING = 1.0f  // +100%
    }
    
    val fontScale: Flow<Float> = context.widgetDataStore.data
        .map { it[FONT_SCALE] ?: DEFAULT_FONT_SCALE }
    
    val letterSpacing: Flow<Float> = context.widgetDataStore.data
        .map { it[LETTER_SPACING] ?: DEFAULT_LETTER_SPACING }
    
    val showCalories: Flow<Boolean> = context.widgetDataStore.data
        .map { (it[SHOW_CALORIES] ?: DEFAULT_SHOW_CALORIES) == 1 }
    
    suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        context.widgetDataStore.edit { it[FONT_SCALE] = clamped }
    }
    
    suspend fun setLetterSpacing(spacing: Float) {
        val clamped = spacing.coerceIn(MIN_LETTER_SPACING, MAX_LETTER_SPACING)
        context.widgetDataStore.edit { it[LETTER_SPACING] = clamped }
    }
    
    suspend fun setShowCalories(show: Boolean) {
        context.widgetDataStore.edit { it[SHOW_CALORIES] = if (show) 1 else 0 }
    }
}

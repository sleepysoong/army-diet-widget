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
        private val SHOW_CALORIES = intPreferencesKey("show_calories")
        
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_SHOW_CALORIES = 1
        
        const val MIN_FONT_SCALE = 0.7f
        const val MAX_FONT_SCALE = 2.0f
    }
    
    val fontScale: Flow<Float> = context.widgetDataStore.data
        .map { it[FONT_SCALE] ?: DEFAULT_FONT_SCALE }
    
    val showCalories: Flow<Boolean> = context.widgetDataStore.data
        .map { (it[SHOW_CALORIES] ?: DEFAULT_SHOW_CALORIES) == 1 }
    
    suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        context.widgetDataStore.edit { it[FONT_SCALE] = clamped }
    }
    
    suspend fun setShowCalories(show: Boolean) {
        context.widgetDataStore.edit { it[SHOW_CALORIES] = if (show) 1 else 0 }
    }
}

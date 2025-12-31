package com.sleepysoong.armydiet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MealWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MealWidget()

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.sleepysoong.armydiet.ACTION_UPDATE_WIDGET"

        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(MealWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        MealWidget().update(context, glanceId)
                    }
                } catch (e: Exception) {
                    // Fallback
                    MealWidget().updateAll(context)
                }
            }
        }
        
        fun updateWidget(context: Context, appWidgetId: Int) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceId = manager.getGlanceIdBy(appWidgetId)
                    MealWidget().update(context, glanceId)
                } catch (e: Exception) {
                    // Fallback to update all
                    updateAllWidgets(context)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_UPDATE_WIDGET) {
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context)
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // 위젯 삭제 시 해당 위젯의 설정 정리
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { appWidgetId ->
                WidgetConfig.clearConfig(context, appWidgetId)
            }
        }
    }
}

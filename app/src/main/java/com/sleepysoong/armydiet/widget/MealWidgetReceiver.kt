package com.sleepysoong.armydiet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 위젯 브로드캐스트 리시버
 * - 시스템 이벤트 수신 (위젯 추가/업데이트/삭제)
 * - 커스텀 업데이트 액션 처리
 */
class MealWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MealWidget()

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.sleepysoong.armydiet.ACTION_UPDATE_WIDGET"

        /**
         * 모든 위젯 업데이트 트리거
         */
        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                MealWidget().updateAll(context)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // 커스텀 업데이트 액션 처리
        if (intent.action == ACTION_UPDATE_WIDGET) {
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 첫 위젯이 추가될 때
        updateAllWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 주기적 업데이트 시
        updateAllWidgets(context)
    }
}

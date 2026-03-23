package com.sleepysoong.armydiet.widget

import android.content.Context

fun interface WidgetUpdateDispatcher {
    fun updateAll()
}

class ContextWidgetUpdateDispatcher(
    private val context: Context
) : WidgetUpdateDispatcher {
    override fun updateAll() {
        MealWidgetReceiver.updateAllWidgets(context)
    }
}

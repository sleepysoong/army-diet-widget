package com.sleepysoong.armydiet

import com.sleepysoong.armydiet.data.local.AppSettings
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher

interface SettingsDependencies {
    val settings: AppSettings
    val mealDao: MealDao
    val widgetUpdateDispatcher: WidgetUpdateDispatcher
}

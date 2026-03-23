package com.sleepysoong.armydiet.testutil

import com.sleepysoong.armydiet.SettingsDependencies
import com.sleepysoong.armydiet.data.local.AppSettings
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher

class FakeSettingsDependencies(
    override val settings: AppSettings,
    override val mealDao: MealDao,
    override val widgetUpdateDispatcher: WidgetUpdateDispatcher
) : SettingsDependencies

class RecordingWidgetUpdateDispatcher : WidgetUpdateDispatcher {
    var count = 0

    override fun updateAll() {
        count += 1
    }
}

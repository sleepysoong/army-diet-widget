package com.sleepysoong.armydiet.worker

import com.sleepysoong.armydiet.data.local.AppSettings
import com.sleepysoong.armydiet.domain.MealDataRepository
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher

interface SyncWorkerDependencies {
    val settings: AppSettings
    val repository: MealDataRepository
    val widgetUpdateDispatcher: WidgetUpdateDispatcher
}

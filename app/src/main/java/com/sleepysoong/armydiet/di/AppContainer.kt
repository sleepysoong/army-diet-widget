package com.sleepysoong.armydiet.di

import android.content.Context
import com.sleepysoong.armydiet.SettingsDependencies
import com.sleepysoong.armydiet.core.SystemAppClock
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.remote.MndApi
import com.sleepysoong.armydiet.data.remote.NetworkModule
import com.sleepysoong.armydiet.domain.MealRepository
import com.sleepysoong.armydiet.widget.ContextWidgetUpdateDispatcher
import com.sleepysoong.armydiet.widget.WidgetUpdateDispatcher

/**
 * 수동 DI 컨테이너
 * - 싱글톤 인스턴스 관리
 * - 앱 전역에서 동일 인스턴스 사용 보장
 */
class AppContainer private constructor(context: Context) : SettingsDependencies {
    
    private val appContext: Context = context.applicationContext
    
    // Database - Lazy initialization
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }
    
    override val mealDao: MealDao by lazy {
        database.mealDao()
    }
    
    // Network
    val api: MndApi by lazy {
        NetworkModule.createApi()
    }
    
    // Preferences
    val preferences: AppPreferences by lazy {
        AppPreferences(appContext)
    }

    override val settings by lazy {
        preferences
    }

    override val widgetUpdateDispatcher: WidgetUpdateDispatcher by lazy {
        ContextWidgetUpdateDispatcher(appContext)
    }
    
    // Repository
    val mealRepository: MealRepository by lazy {
        MealRepository(mealDao, api, preferences, { baseUrl ->
            NetworkModule.createExternalApi(baseUrl)
        }, SystemAppClock)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null
        
        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}

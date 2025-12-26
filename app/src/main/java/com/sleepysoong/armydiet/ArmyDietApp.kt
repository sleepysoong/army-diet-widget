package com.sleepysoong.armydiet

import android.app.Application
import com.sleepysoong.armydiet.di.AppContainer

class ArmyDietApp : Application() {
    
    lateinit var container: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        container = AppContainer.getInstance(this)
    }
}

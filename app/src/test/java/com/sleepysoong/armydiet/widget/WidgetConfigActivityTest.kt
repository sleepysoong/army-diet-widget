package com.sleepysoong.armydiet.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class WidgetConfigActivityTest {
    @Test
    fun invalidWidgetId_finishesImmediately() {
        val activity = Robolectric.buildActivity(WidgetConfigActivity::class.java)
            .create()
            .get()

        assertTrue(activity.isFinishing)
        assertTrue(Shadows.shadowOf(activity).resultCode == Activity.RESULT_CANCELED)
    }

    @Test
    fun validWidgetId_staysOpenForConfiguration() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 101)
        }

        val activity = Robolectric.buildActivity(WidgetConfigActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()

        assertFalse(activity.isFinishing)
        assertTrue(Shadows.shadowOf(activity).resultCode == Activity.RESULT_CANCELED)
    }
}

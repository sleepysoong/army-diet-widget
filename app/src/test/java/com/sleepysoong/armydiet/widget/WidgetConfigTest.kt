package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetConfigTest {
    private lateinit var context: Context
    private val firstWidgetId = 1001
    private val secondWidgetId = 2002

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        WidgetConfig.clearConfig(context, firstWidgetId)
        WidgetConfig.clearConfig(context, secondWidgetId)
    }

    @After
    fun tearDown() = runBlocking {
        WidgetConfig.clearConfig(context, firstWidgetId)
        WidgetConfig.clearConfig(context, secondWidgetId)
    }

    @Test
    fun widgetSettings_areStoredPerWidgetAndClamped() = runBlocking {
        val first = WidgetConfig(context, firstWidgetId)
        val second = WidgetConfig(context, secondWidgetId)

        first.setFontScale(9.0f)
        first.setTagScale(0.2f)
        first.setHeaderScale(9.0f)
        first.setShowCalories(false)

        second.setFontScale(0.1f)
        second.setTagScale(1.7f)
        second.setHeaderScale(2.8f)
        second.setShowCalories(true)

        assertEquals(WidgetConfig.MAX_FONT_SCALE, first.fontScale.first(), 0.0001f)
        assertEquals(WidgetConfig.MIN_TAG_SCALE, first.tagScale.first(), 0.0001f)
        assertEquals(WidgetConfig.MAX_HEADER_SCALE, first.headerScale.first(), 0.0001f)
        assertFalse(first.showCalories.first())

        assertEquals(WidgetConfig.MIN_FONT_SCALE, second.fontScale.first(), 0.0001f)
        assertEquals(1.7f, second.tagScale.first(), 0.0001f)
        assertEquals(2.8f, second.headerScale.first(), 0.0001f)
        assertTrue(second.showCalories.first())
    }

    @Test
    fun clearConfig_removesOnlySpecifiedWidgetSettings() = runBlocking {
        val first = WidgetConfig(context, firstWidgetId)
        val second = WidgetConfig(context, secondWidgetId)

        first.setFontScale(1.4f)
        first.setShowCalories(false)
        second.setFontScale(1.8f)
        second.setShowCalories(true)

        WidgetConfig.clearConfig(context, firstWidgetId)

        assertEquals(WidgetConfig.DEFAULT_FONT_SCALE, first.fontScale.first(), 0.0001f)
        assertTrue(first.showCalories.first())

        assertEquals(1.8f, second.fontScale.first(), 0.0001f)
        assertTrue(second.showCalories.first())
    }
}

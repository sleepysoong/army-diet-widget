package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetConfigScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private val widgetId = 333

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        WidgetConfig.clearConfig(context, widgetId)
    }

    @After
    fun tearDown() = runBlocking {
        WidgetConfig.clearConfig(context, widgetId)
    }

    @Test
    fun saveButton_disabledForInvalidFontInput_andEnabledForValidInput() {
        val config = WidgetConfig(context, widgetId)

        composeRule.setContent {
            WidgetConfigScreen(config = config, appWidgetId = widgetId, onSaveComplete = {})
        }

        composeRule.onNodeWithTag("widget_font_input").performTextClearance()
        composeRule.onNodeWithTag("widget_font_input").performTextInput("300")
        composeRule.onNodeWithTag("widget_save_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("widget_font_input").performTextClearance()
        composeRule.onNodeWithTag("widget_font_input").performTextInput("120")
        composeRule.onNodeWithTag("widget_save_button").assertIsEnabled()
    }
}

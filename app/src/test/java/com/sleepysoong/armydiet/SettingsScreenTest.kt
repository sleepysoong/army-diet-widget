package com.sleepysoong.armydiet

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.testutil.FakeAppSettings
import com.sleepysoong.armydiet.testutil.FakeMealDao
import com.sleepysoong.armydiet.testutil.FakeSettingsDependencies
import com.sleepysoong.armydiet.testutil.RecordingWidgetUpdateDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreen_switchesFromLocalToExternalAndShowsEndpointField() {
        val settings = FakeAppSettings(mealSource = AppPreferences.SOURCE_LOCAL)
        val dependencies = FakeSettingsDependencies(
            settings = settings,
            mealDao = FakeMealDao(),
            widgetUpdateDispatcher = RecordingWidgetUpdateDispatcher()
        )

        composeRule.setContent {
            SettingsScreen(dependencies = dependencies, onBack = {})
        }

        composeRule.onNodeWithTag("settings_local_unit_code").assertExists()
        composeRule.onNodeWithTag("settings_external_endpoint").assertDoesNotExist()

        runBlocking {
            settings.setMealSource(AppPreferences.SOURCE_EXTERNAL)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_external_endpoint").assertExists()
        composeRule.onNodeWithTag("settings_local_unit_code").assertDoesNotExist()
    }

    @Test
    fun settingsScreen_switchesBackToLocalAndShowsUnitCodeField() {
        val settings = FakeAppSettings(mealSource = AppPreferences.SOURCE_EXTERNAL)
        val dependencies = FakeSettingsDependencies(
            settings = settings,
            mealDao = FakeMealDao(),
            widgetUpdateDispatcher = RecordingWidgetUpdateDispatcher()
        )

        composeRule.setContent {
            SettingsScreen(dependencies = dependencies, onBack = {})
        }

        composeRule.onNodeWithTag("settings_external_endpoint").assertExists()
        composeRule.onNodeWithTag("settings_local_unit_code").assertDoesNotExist()

        runBlocking {
            settings.setMealSource(AppPreferences.SOURCE_LOCAL)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_local_unit_code").assertExists()
        composeRule.onNodeWithTag("settings_external_endpoint").assertDoesNotExist()
    }
}

package com.sleepysoong.armydiet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.armydiet.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set result to CANCELED in case user backs out
        setResult(RESULT_CANCELED)
        
        // Get widget ID from intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        val config = WidgetConfig(applicationContext)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        config = config,
                        onSave = { saveAndFinish() }
                    )
                }
            }
        }
    }
    
    private fun saveAndFinish() {
        // Update widget
        MealWidgetReceiver.updateAllWidgets(applicationContext)
        
        // Return success
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetConfigScreen(config: WidgetConfig, onSave: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    var fontScale by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_FONT_SCALE) }
    var bgAlpha by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_BG_ALPHA) }
    var showCalories by remember { mutableStateOf(true) }
    
    // Load current values
    LaunchedEffect(Unit) {
        fontScale = config.fontScale.first()
        bgAlpha = config.backgroundAlpha.first()
        showCalories = config.showCalories.first()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "위젯 설정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Font Scale
        ConfigSlider(
            title = "글자 크기",
            value = fontScale,
            valueRange = WidgetConfig.MIN_FONT_SCALE..WidgetConfig.MAX_FONT_SCALE,
            valueLabel = "${(fontScale * 100).toInt()}%",
            onValueChange = { fontScale = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Background Alpha
        ConfigSlider(
            title = "배경 투명도",
            value = bgAlpha,
            valueRange = 0f..1f,
            valueLabel = "${(bgAlpha * 100).toInt()}%",
            onValueChange = { bgAlpha = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show Calories
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "칼로리 표시",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = showCalories,
                onCheckedChange = { showCalories = it }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Save Button
        Button(
            onClick = {
                scope.launch {
                    config.setFontScale(fontScale)
                    config.setBackgroundAlpha(bgAlpha)
                    config.setShowCalories(showCalories)
                    onSave()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }
    }
}

@Composable
private fun ConfigSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

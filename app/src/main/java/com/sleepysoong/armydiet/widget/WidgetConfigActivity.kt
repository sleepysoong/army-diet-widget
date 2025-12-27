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
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.sleepysoong.armydiet.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setResult(RESULT_CANCELED)
        
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
                        appWidgetId = appWidgetId,
                        onSaveComplete = { finishWithSuccess() }
                    )
                }
            }
        }
    }
    
    private fun finishWithSuccess() {
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetConfigScreen(
    config: WidgetConfig,
    appWidgetId: Int,
    onSaveComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var fontScale by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_FONT_SCALE) }
    var bgAlpha by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_BG_ALPHA) }
    var showCalories by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
        
        ConfigSlider(
            title = "글자 크기",
            value = fontScale,
            valueRange = WidgetConfig.MIN_FONT_SCALE..WidgetConfig.MAX_FONT_SCALE,
            valueLabel = "${(fontScale * 100).toInt()}%",
            onValueChange = { fontScale = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ConfigSlider(
            title = "배경 투명도",
            value = bgAlpha,
            valueRange = 0f..1f,
            valueLabel = "${(bgAlpha * 100).toInt()}%",
            onValueChange = { bgAlpha = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        Button(
            onClick = {
                if (!isSaving) {
                    isSaving = true
                    scope.launch {
                        // 1. 설정 저장
                        config.setFontScale(fontScale)
                        config.setBackgroundAlpha(bgAlpha)
                        config.setShowCalories(showCalories)
                        
                        // 2. 저장 완료 대기
                        delay(100)
                        
                        // 3. 위젯 강제 업데이트
                        try {
                            val manager = GlanceAppWidgetManager(context)
                            val glanceIds = manager.getGlanceIds(MealWidget::class.java)
                            glanceIds.forEach { glanceId ->
                                MealWidget().update(context, glanceId)
                            }
                        } catch (e: Exception) {
                            // Fallback - broadcast update
                            MealWidgetReceiver.updateAllWidgets(context)
                        }
                        
                        // 4. 업데이트 완료 대기
                        delay(200)
                        
                        // 5. 종료
                        onSaveComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSaving) "저장 중..." else "저장")
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

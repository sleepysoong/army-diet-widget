package com.sleepysoong.armydiet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        
        val config = WidgetConfig(applicationContext, appWidgetId)
        
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
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var fontScale by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_FONT_SCALE) }
    var tagScale by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_TAG_SCALE) }
    var headerScale by remember { mutableFloatStateOf(WidgetConfig.DEFAULT_HEADER_SCALE) }
    var showCalories by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        fontScale = config.fontScale.first()
        tagScale = config.tagScale.first()
        headerScale = config.headerScale.first()
        showCalories = config.showCalories.first()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "위젯 설정",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 글자 크기
        ConfigSlider(
            title = "기본 글자 크기",
            value = fontScale,
            valueRange = WidgetConfig.MIN_FONT_SCALE..WidgetConfig.MAX_FONT_SCALE,
            valueLabel = "${(fontScale * 100).toInt()}%",
            onValueChange = { fontScale = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 태그 크기 비율
        ConfigSlider(
            title = "태그 크기 비율",
            value = tagScale,
            valueRange = WidgetConfig.MIN_TAG_SCALE..WidgetConfig.MAX_TAG_SCALE,
            valueLabel = "x${String.format("%.1f", tagScale)}",
            onValueChange = { tagScale = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 날짜 크기 비율
        ConfigSlider(
            title = "날짜 크기 비율",
            value = headerScale,
            valueRange = WidgetConfig.MIN_HEADER_SCALE..WidgetConfig.MAX_HEADER_SCALE,
            valueLabel = "x${String.format("%.1f", headerScale)}",
            onValueChange = { headerScale = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 칼로리 표시
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (!isSaving) {
                    isSaving = true
                    scope.launch {
                        config.setFontScale(fontScale)
                        config.setTagScale(tagScale)
                        config.setHeaderScale(headerScale)
                        config.setShowCalories(showCalories)
                        
                        // DataStore 저장이 완료될 때까지 잠시 대기
                        delay(100)
                        
                        // 해당 위젯만 업데이트 요청
                        MealWidgetReceiver.updateWidget(context, appWidgetId)
                        
                        delay(200)
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

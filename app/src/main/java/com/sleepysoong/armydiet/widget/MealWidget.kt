package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.sleepysoong.armydiet.MainActivity
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MealWidget : GlanceAppWidget() {

    companion object {
        private val SIZE_SMALL = DpSize(110.dp, 60.dp)
        private val SIZE_MEDIUM = DpSize(200.dp, 120.dp)
        private val SIZE_LARGE = DpSize(280.dp, 180.dp)
        private val SIZE_XLARGE = DpSize(360.dp, 240.dp)
        private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        
        provideContent {
            GlanceTheme {
                WidgetContent(data = data, size = LocalSize.current)
            }
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        val container = AppContainer.getInstance(context)
        val config = WidgetConfig(context)
        
        val meal = runCatching {
            val dateStr = getTargetDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            container.mealDao.getMeal(dateStr)
        }.getOrNull()
        
        WidgetData(
            meal = meal,
            displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M/d")),
            currentMeal = getCurrentMealType(),
            fontScale = config.fontScale.first(),
            showCalories = config.showCalories.first()
        )
    }

    private fun getTargetDate(): LocalDate =
        if (LocalTime.now().hour >= 18) LocalDate.now().plusDays(1) else LocalDate.now()

    private fun getCurrentMealType(): MealType {
        val hour = LocalTime.now().hour
        return when {
            hour < 9 -> MealType.BREAKFAST
            hour < 14 -> MealType.LUNCH
            else -> MealType.DINNER
        }
    }
}

private data class WidgetData(
    val meal: MealEntity?,
    val displayDate: String,
    val currentMeal: MealType,
    val fontScale: Float,
    val showCalories: Boolean
)

enum class MealType(val label: String) {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식")
}

@Composable
private fun WidgetContent(data: WidgetData, size: DpSize) {
    val isSmall = size.width < 200.dp || size.height < 120.dp
    val isLarge = size.width >= 280.dp && size.height >= 180.dp
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // 날짜
        Text(
            text = data.displayDate,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = (if (isSmall) 11 else 13).sp * data.fontScale,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.None
            )
        )
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        if (isSmall) {
            // 작은 위젯: 현재 끼니만
            CompactContent(data)
        } else {
            // 일반/큰 위젯: 전체 끼니
            FullContent(data, isLarge)
        }
    }
}

@Composable
private fun CompactContent(data: WidgetData) {
    val content = getMealContent(data.meal, data.currentMeal)
    
    Text(
        text = "${data.currentMeal.label}: $content",
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = 10.sp * data.fontScale,
            fontWeight = FontWeight.Normal
        ),
        maxLines = 3
    )
}

@Composable
private fun FullContent(data: WidgetData, isLarge: Boolean) {
    val fontSize = (if (isLarge) 12 else 11).sp * data.fontScale
    val labelWidth = if (isLarge) 32.dp else 28.dp
    
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        MealType.entries.forEach { type ->
            val content = getMealContent(data.meal, type)
            val isCurrent = type == data.currentMeal
            
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 라벨 (고정 폭)
                Text(
                    text = type.label,
                    style = TextStyle(
                        color = if (isCurrent) GlanceTheme.colors.primary else GlanceTheme.colors.onBackground,
                        fontSize = fontSize,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    modifier = GlanceModifier.width(labelWidth)
                )
                
                // 메뉴 (남은 공간)
                Text(
                    text = content,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = if (isLarge) 3 else 2
                )
            }
            
            if (type != MealType.DINNER) {
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
        
        // 칼로리
        if (data.showCalories) {
            formatCalories(data.meal?.sumCal)?.let { cal ->
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = cal,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (fontSize.value * 0.9f).sp
                    )
                )
            }
        }
    }
}

private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")

private fun getMealContent(meal: MealEntity?, type: MealType): String {
    if (meal == null) return "-"
    val content = when (type) {
        MealType.BREAKFAST -> meal.breakfast
        MealType.LUNCH -> meal.lunch
        MealType.DINNER -> meal.dinner
    }
    if (content.isBlank() || content == "메뉴 정보 없음") return "-"
    return ALLERGY_REGEX.replace(content, "").replace("  ", " ").trim()
}

private fun formatCalories(sumCal: String?): String? {
    if (sumCal.isNullOrBlank()) return null
    val cleaned = sumCal.replace("kcal", "").replace("Kcal", "").replace("KCAL", "").trim()
    val value = cleaned.toDoubleOrNull() ?: return null
    return "${value.toInt()} kcal"
}

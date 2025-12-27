package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.unit.ColorProvider
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
    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁")
}

@Composable
private fun WidgetContent(data: WidgetData, size: DpSize) {
    val isSmall = size.width < 200.dp || size.height < 120.dp
    val isLarge = size.width >= 280.dp && size.height >= 180.dp
    
    // 다크 그린 색상 정의
    val darkGreen = ColorProvider(Color(0xFF1B5E20))
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 상단: 날짜 (칼로리) - 중앙 정렬
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.displayDate,
                style = TextStyle(
                    color = darkGreen,
                    fontSize = (if (isSmall) 12 else 14).sp * data.fontScale,
                    fontWeight = FontWeight.Bold
                )
            )
            
            if (data.showCalories) {
                formatCalories(data.meal?.sumCal)?.let { cal ->
                    Text(
                        text = " ($cal)",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (if (isSmall) 11 else 13).sp * data.fontScale
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        if (isSmall) {
            CompactContent(data, darkGreen)
        } else {
            FullContent(data, isLarge, darkGreen)
        }
    }
}

@Composable
private fun CompactContent(data: WidgetData, themeColor: ColorProvider) {
    val content = getMealContent(data.meal, data.currentMeal)
    val fontSize = 16.sp * data.fontScale // 대폭 키움
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        MealTag(data.currentMeal.label, themeColor, data.fontScale)
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = content,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = 2
        )
    }
}

@Composable
private fun FullContent(data: WidgetData, isLarge: Boolean, themeColor: ColorProvider) {
    val fontSize = (if (isLarge) 20 else 18).sp * data.fontScale // 대폭 키움 (기존 대비 약 2배)
    
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MealType.entries.forEach { type ->
            val content = getMealContent(data.meal, type)
            val isCurrent = type == data.currentMeal
            
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MealTag(
                    label = type.label,
                    color = if (isCurrent) themeColor else ColorProvider(Color(0xFF666666)),
                    fontScale = data.fontScale,
                    isCurrent = isCurrent
                )
                
                Spacer(modifier = GlanceModifier.height(2.dp))
                
                Text(
                    text = content,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = fontSize,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = if (isLarge) 3 else 2
                )
            }
        }
    }
}

@Composable
private fun MealTag(label: String, color: ColorProvider, fontScale: Float, isCurrent: Boolean = true) {
    Box(
        modifier = GlanceModifier
            .background(if (isCurrent) color else ColorProvider(Color.Transparent))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .cornerRadius(16.dp), // 완전 둥근 모서리
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (isCurrent) ColorProvider(Color.White) else color,
                fontSize = 12.sp * fontScale,
                fontWeight = FontWeight.Bold
            )
        )
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

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
import androidx.glance.appwidget.lazy.LazyRow
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.sleepysoong.armydiet.MainActivity
import com.sleepysoong.armydiet.R
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
            displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M월 d일")),
            currentMeal = getCurrentMealType(),
            fontScale = config.fontScale.first(),
            showCalories = config.showCalories.first(),
            keywords = container.preferences.highlightKeywords.first()
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
    val showCalories: Boolean,
    val keywords: Set<String>
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
        // 상단: 날짜 (칼로리) - 중앙 정렬 및 크기 확대
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val headerFontSize = (if (isSmall) 24 else 28).sp * data.fontScale
            
            Text(
                text = data.displayDate,
                style = TextStyle(
                    color = darkGreen,
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold
                )
            )
            
            if (data.showCalories) {
                formatCalories(data.meal?.sumCal)?.let { cal ->
                    Text(
                        text = " ($cal)",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = headerFontSize // 날짜와 동일한 크기
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = GlanceModifier.height(12.dp))
        
        // 메뉴 섹션: 왼쪽 패딩 추가로 날짜 정렬과 구분
        Box(modifier = GlanceModifier.fillMaxWidth().padding(start = 12.dp)) {
            if (isSmall) {
                CompactContent(data, darkGreen)
            } else {
                FullContent(data, isLarge, darkGreen)
            }
        }
    }
}

@Composable
private fun CompactContent(data: WidgetData, themeColor: ColorProvider) {
    val content = getMealContent(data.meal, data.currentMeal)
    val menus = content.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
    val fontSize = 16.sp * data.fontScale
    
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        MealTag(
            label = data.currentMeal.label, 
            activeColor = themeColor, 
            fontSize = fontSize,
            isCurrent = true
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        // Menu List (Horizontal Scroll)
        LazyRow(modifier = GlanceModifier.fillMaxWidth()) {
            items(menus) { menu ->
                val isDelicious = data.keywords.any { menu.contains(it) }
                MenuChip(menu, isDelicious, fontSize)
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun FullContent(data: WidgetData, isLarge: Boolean, themeColor: ColorProvider) {
    val fontSize = (if (isLarge) 20 else 18).sp * data.fontScale
    
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        MealType.entries.forEach { type ->
            val content = getMealContent(data.meal, type)
            val menus = content.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
            val isCurrent = type == data.currentMeal
            
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MealTag(
                    label = type.label,
                    activeColor = themeColor,
                    fontSize = fontSize,
                    isCurrent = isCurrent
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                LazyRow(modifier = GlanceModifier.fillMaxWidth()) {
                    items(menus) { menu ->
                        val isDelicious = data.keywords.any { menu.contains(it) }
                        MenuChip(menu, isDelicious, fontSize)
                        Spacer(modifier = GlanceModifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTag(
    label: String, 
    activeColor: ColorProvider, 
    fontSize: TextUnit, 
    isCurrent: Boolean
) {
    val backgroundColor = if (isCurrent) activeColor else ColorProvider(Color(0xFFE0E0E0))
    val textColor = if (isCurrent) ColorProvider(Color.White) else ColorProvider(Color(0xFF424242))
    
    Box(
        modifier = GlanceModifier
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun MenuChip(text: String, isDelicious: Boolean, fontSize: TextUnit) {
    val backgroundColor = if (isDelicious) ColorProvider(Color(0xFF1B5E20)) else ColorProvider(Color.Transparent)
    val textColor = if (isDelicious) ColorProvider(Color(0xFFF1F8E9)) else GlanceTheme.colors.onBackground
    val fontWeight = if (isDelicious) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = GlanceModifier
            .background(backgroundColor)
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight
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

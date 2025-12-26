package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.sleepysoong.armydiet.MainActivity
import com.sleepysoong.armydiet.R
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.MealEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 군 급식 위젯 - 반응형 크기 지원
 * 
 * 크기별 레이아웃:
 * - Small (2x1~2x2): 오늘 날짜 + 현재 끼니만
 * - Medium (3x2~4x2): 날짜 + 조식/중식/석식 요약
 * - Large (4x3+): 전체 메뉴 상세 표시
 */
class MealWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(100.dp, 48.dp)
        private val MEDIUM = DpSize(180.dp, 100.dp)
        private val LARGE = DpSize(250.dp, 180.dp)
        private val EXTRA_LARGE = DpSize(300.dp, 280.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE, EXTRA_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val meal = loadMealData(context)
        val targetDate = getTargetDate()
        val displayDate = targetDate.format(DateTimeFormatter.ofPattern("M월 d일 (E)"))
        val currentMealType = getCurrentMealType()

        provideContent {
            val size = LocalSize.current
            GlanceTheme {
                WidgetContent(
                    meal = meal,
                    displayDate = displayDate,
                    currentMealType = currentMealType,
                    size = size
                )
            }
        }
    }

    private suspend fun loadMealData(context: Context): MealEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                val targetDate = getTargetDate()
                val dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                database.mealDao().getMeal(dateStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getTargetDate(): LocalDate {
        val nowTime = LocalTime.now()
        val nowDate = LocalDate.now()
        return if (nowTime.hour >= 18) nowDate.plusDays(1) else nowDate
    }

    private fun getCurrentMealType(): MealType {
        val hour = LocalTime.now().hour
        return when {
            hour < 9 -> MealType.BREAKFAST
            hour < 14 -> MealType.LUNCH
            else -> MealType.DINNER
        }
    }
}

enum class MealType(val label: String, val emoji: String) {
    BREAKFAST("조식", "🌅"),
    LUNCH("중식", "☀️"),
    DINNER("석식", "🌙")
}

@Composable
private fun WidgetContent(
    meal: MealEntity?,
    displayDate: String,
    currentMealType: MealType,
    size: DpSize
) {
    val isSmall = size.width < 180.dp || size.height < 100.dp
    val isMedium = size.width < 250.dp || size.height < 180.dp
    val isLarge = size.width >= 250.dp && size.height >= 180.dp
    val isExtraLarge = size.width >= 300.dp && size.height >= 280.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(if (isSmall) 8.dp else 12.dp)
    ) {
        when {
            isExtraLarge -> ExtraLargeLayout(meal, displayDate, currentMealType)
            isLarge -> LargeLayout(meal, displayDate, currentMealType)
            isMedium -> MediumLayout(meal, displayDate, currentMealType)
            else -> SmallLayout(meal, displayDate, currentMealType)
        }
    }
}

/**
 * Small 레이아웃: 현재 끼니만 표시
 */
@Composable
private fun SmallLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMealType: MealType
) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${currentMealType.emoji} ${currentMealType.label}",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = getCurrentMealContent(meal, currentMealType).take(30) + 
                   if (getCurrentMealContent(meal, currentMealType).length > 30) "..." else "",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 11.sp
            ),
            maxLines = 2
        )
    }
}

/**
 * Medium 레이아웃: 날짜 + 3끼 요약
 */
@Composable
private fun MediumLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMealType: MealType
) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // 헤더
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🍚 짬수첩",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = displayDate,
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 10.sp
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(6.dp))
        
        // 3끼 요약 (가로 배치)
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
        ) {
            MealType.entries.forEach { type ->
                MealSummaryItem(
                    mealType = type,
                    content = getCurrentMealContent(meal, type),
                    isActive = type == currentMealType,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

/**
 * Large 레이아웃: 전체 상세 표시
 */
@Composable
private fun LargeLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMealType: MealType
) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        WidgetHeader(displayDate)
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
        ) {
            MealType.entries.forEach { type ->
                MealDetailItem(
                    mealType = type,
                    content = getCurrentMealContent(meal, type),
                    isActive = type == currentMealType,
                    maxLines = 2
                )
                if (type != MealType.DINNER) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Extra Large 레이아웃: 풀 상세 + 칼로리
 */
@Composable
private fun ExtraLargeLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMealType: MealType
) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        WidgetHeader(displayDate)
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
        ) {
            MealType.entries.forEach { type ->
                MealDetailItem(
                    mealType = type,
                    content = getCurrentMealContent(meal, type),
                    isActive = type == currentMealType,
                    maxLines = 3
                )
                if (type != MealType.DINNER) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
        
        // 칼로리
        if (!meal?.sumCal.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "🔥 ${meal?.sumCal} kcal",
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WidgetHeader(displayDate: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🍚 짬수첩",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = displayDate,
            style = TextStyle(
                color = GlanceTheme.colors.secondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun MealSummaryItem(
    mealType: MealType,
    content: String,
    isActive: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = mealType.emoji,
            style = TextStyle(fontSize = 12.sp)
        )
        Text(
            text = content.split(",").firstOrNull()?.trim()?.take(6) ?: "정보없음",
            style = TextStyle(
                color = if (isActive) GlanceTheme.colors.primary else GlanceTheme.colors.onBackground,
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MealDetailItem(
    mealType: MealType,
    content: String,
    isActive: Boolean,
    maxLines: Int
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(if (isActive) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant)
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${mealType.emoji} ${mealType.label}",
            style = TextStyle(
                color = if (isActive) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.width(56.dp)
        )
        Text(
            text = formatMenuContent(content),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 11.sp
            ),
            maxLines = maxLines,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

private fun getCurrentMealContent(meal: MealEntity?, mealType: MealType): String {
    if (meal == null) return "데이터 없음"
    return when (mealType) {
        MealType.BREAKFAST -> meal.breakfast
        MealType.LUNCH -> meal.lunch
        MealType.DINNER -> meal.dinner
    }.let { 
        if (it.isBlank() || it == "메뉴 정보 없음") "정보 없음" else it 
    }
}

private fun formatMenuContent(content: String): String {
    return content.replace(", ", " · ")
}

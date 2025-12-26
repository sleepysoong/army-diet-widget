package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
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
        private val SIZE_COMPACT = DpSize(110.dp, 60.dp)
        private val SIZE_SMALL = DpSize(180.dp, 110.dp)
        private val SIZE_MEDIUM = DpSize(250.dp, 160.dp)
        private val SIZE_LARGE = DpSize(320.dp, 220.dp)
        private val SIZE_EXPANDED = DpSize(400.dp, 300.dp)
        
        private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_EXPANDED)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        
        provideContent {
            GlanceTheme {
                AdaptiveWidgetContent(data = data, size = LocalSize.current)
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
            displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M/d (E)")),
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

data class WidgetConfigData(
    val fontScale: Float = 1.0f,
    val bgAlpha: Float = 1.0f,
    val showCalories: Boolean = true
)

enum class MealType(val label: String) {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식")
}

// Adaptive sizing based on widget dimensions
private data class AdaptiveSize(
    val titleFont: TextUnit,
    val labelFont: TextUnit,
    val contentFont: TextUnit,
    val tagPadding: Dp,
    val tagRadius: Dp,
    val spacing: Dp,
    val outerPadding: Dp,
    val maxMenuLines: Int,
    val showLabel: Boolean,
    val isCompact: Boolean
)

private fun calculateAdaptiveSize(size: DpSize, fontScale: Float): AdaptiveSize {
    val width = size.width
    val height = size.height
    
    return when {
        // Compact: 현재 끼니만
        width < 180.dp || height < 110.dp -> AdaptiveSize(
            titleFont = (11 * fontScale).sp,
            labelFont = (10 * fontScale).sp,
            contentFont = (10 * fontScale).sp,
            tagPadding = 4.dp,
            tagRadius = 6.dp,
            spacing = 4.dp,
            outerPadding = 8.dp,
            maxMenuLines = 2,
            showLabel = false,
            isCompact = true
        )
        // Small: 3끼 한 줄씩
        width < 250.dp || height < 160.dp -> AdaptiveSize(
            titleFont = (12 * fontScale).sp,
            labelFont = (11 * fontScale).sp,
            contentFont = (11 * fontScale).sp,
            tagPadding = 5.dp,
            tagRadius = 8.dp,
            spacing = 5.dp,
            outerPadding = 10.dp,
            maxMenuLines = 1,
            showLabel = true,
            isCompact = false
        )
        // Medium: 3끼 2줄씩
        width < 320.dp || height < 220.dp -> AdaptiveSize(
            titleFont = (13 * fontScale).sp,
            labelFont = (12 * fontScale).sp,
            contentFont = (12 * fontScale).sp,
            tagPadding = 6.dp,
            tagRadius = 10.dp,
            spacing = 6.dp,
            outerPadding = 12.dp,
            maxMenuLines = 2,
            showLabel = true,
            isCompact = false
        )
        // Large: 3끼 상세
        width < 400.dp || height < 300.dp -> AdaptiveSize(
            titleFont = (14 * fontScale).sp,
            labelFont = (13 * fontScale).sp,
            contentFont = (13 * fontScale).sp,
            tagPadding = 8.dp,
            tagRadius = 12.dp,
            spacing = 8.dp,
            outerPadding = 14.dp,
            maxMenuLines = 3,
            showLabel = true,
            isCompact = false
        )
        // Expanded: Fold 등 대형 화면
        else -> AdaptiveSize(
            titleFont = (16 * fontScale).sp,
            labelFont = (14 * fontScale).sp,
            contentFont = (14 * fontScale).sp,
            tagPadding = 10.dp,
            tagRadius = 14.dp,
            spacing = 10.dp,
            outerPadding = 16.dp,
            maxMenuLines = 4,
            showLabel = true,
            isCompact = false
        )
    }
}

@Composable
private fun AdaptiveWidgetContent(data: WidgetData, size: DpSize) {
    val adaptive = calculateAdaptiveSize(size, data.fontScale)
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(adaptive.outerPadding)
    ) {
        // Header: 날짜
        Text(
            text = data.displayDate,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = adaptive.titleFont,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = GlanceModifier.height(adaptive.spacing))
        
        if (adaptive.isCompact) {
            // Compact: 현재 끼니만 표시
            CompactMealContent(data, adaptive)
        } else {
            // Standard: 모든 끼니 표시
            StandardMealContent(data, adaptive)
        }
        
        // 칼로리
        if (data.showCalories && !adaptive.isCompact) {
            formatCalories(data.meal?.sumCal)?.let { cal ->
                Spacer(modifier = GlanceModifier.height(adaptive.spacing))
                Text(
                    text = cal,
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = (adaptive.contentFont.value * 0.85f).sp
                    )
                )
            }
        }
    }
}

@Composable
private fun CompactMealContent(data: WidgetData, adaptive: AdaptiveSize) {
    val content = getMealContent(data.meal, data.currentMeal)
    
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // 현재 끼니 라벨
        MealTag(
            label = data.currentMeal.label,
            isActive = true,
            adaptive = adaptive
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = content,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = adaptive.contentFont
            ),
            maxLines = adaptive.maxMenuLines
        )
    }
}

@Composable
private fun StandardMealContent(data: WidgetData, adaptive: AdaptiveSize) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        MealType.entries.forEach { type ->
            val isActive = type == data.currentMeal
            val content = getMealContent(data.meal, type)
            
            MealRow(
                type = type,
                content = content,
                isActive = isActive,
                adaptive = adaptive
            )
            
            if (type != MealType.DINNER) {
                Spacer(modifier = GlanceModifier.height(adaptive.spacing))
            }
        }
    }
}

@Composable
private fun MealRow(
    type: MealType,
    content: String,
    isActive: Boolean,
    adaptive: AdaptiveSize
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 끼니 태그
        MealTag(label = type.label, isActive = isActive, adaptive = adaptive)
        
        Spacer(modifier = GlanceModifier.width(8.dp))
        
        // 메뉴 내용 (개별 태그로)
        MenuChips(content = content, adaptive = adaptive)
    }
}

@Composable
private fun MealTag(label: String, isActive: Boolean, adaptive: AdaptiveSize) {
    Box(
        modifier = GlanceModifier
            .background(
                if (isActive) GlanceTheme.colors.primary
                else ColorProvider(R.color.widget_tag_bg)
            )
            .cornerRadius(adaptive.tagRadius)
            .padding(horizontal = adaptive.tagPadding, vertical = (adaptive.tagPadding.value * 0.6f).dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (isActive) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onBackground,
                fontSize = adaptive.labelFont,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun MenuChips(content: String, adaptive: AdaptiveSize) {
    // 메뉴를 개별 칩으로 표시 (노션 스타일)
    val items = content.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "-" }
        .take(adaptive.maxMenuLines * 3) // 라인 수에 맞게 제한
    
    if (items.isEmpty()) {
        MenuChip(text = "-", adaptive = adaptive)
        return
    }
    
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Glance에서 FlowRow 없으므로 단순 Row로 처리
        items.take(4).forEachIndexed { index, item ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(4.dp))
            MenuChip(text = item.take(8), adaptive = adaptive) // 길이 제한
        }
        if (items.size > 4) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            MenuChip(text = "+${items.size - 4}", adaptive = adaptive)
        }
    }
}

@Composable
private fun MenuChip(text: String, adaptive: AdaptiveSize) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(R.color.widget_chip_bg))
            .cornerRadius((adaptive.tagRadius.value * 0.7f).dp)
            .padding(horizontal = (adaptive.tagPadding.value * 0.8f).dp, vertical = (adaptive.tagPadding.value * 0.4f).dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = (adaptive.contentFont.value * 0.9f).sp
            ),
            maxLines = 1
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

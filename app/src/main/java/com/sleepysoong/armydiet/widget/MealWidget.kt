package com.sleepysoong.armydiet.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
        private val SIZE_SMALL = DpSize(100.dp, 48.dp)
        private val SIZE_MEDIUM = DpSize(180.dp, 100.dp)
        private val SIZE_LARGE = DpSize(250.dp, 180.dp)
        private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")
    }

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE))

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
        val config = container.widgetConfig
        
        val meal = runCatching {
            val dateStr = getTargetDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            container.mealDao.getMeal(dateStr)
        }.getOrNull()
        
        WidgetData(
            meal = meal,
            displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M/d")),
            currentMeal = getCurrentMealType(),
            fontScale = config.fontScale.first(),
            bgAlpha = config.backgroundAlpha.first(),
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
    val bgAlpha: Float,
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

@Composable
private fun WidgetContent(data: WidgetData, size: DpSize) {
    val layout = when {
        size.width < 180.dp || size.height < 100.dp -> WidgetLayout.SMALL
        size.width >= 250.dp && size.height >= 180.dp -> WidgetLayout.LARGE
        else -> WidgetLayout.MEDIUM
    }
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(6.dp)
    ) {
        when (layout) {
            WidgetLayout.SMALL -> SmallLayout(data)
            WidgetLayout.MEDIUM -> MediumLayout(data)
            WidgetLayout.LARGE -> LargeLayout(data)
        }
    }
}

private enum class WidgetLayout { SMALL, MEDIUM, LARGE }

// Font sizes based on scale
private fun titleSize(scale: Float): TextUnit = (10 * scale).sp
private fun labelSize(scale: Float): TextUnit = (9 * scale).sp
private fun contentSize(scale: Float): TextUnit = (9 * scale).sp
private fun smallSize(scale: Float): TextUnit = (8 * scale).sp

@Composable
private fun SmallLayout(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = data.currentMeal.label,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = labelSize(data.fontScale),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = getMealContent(data.meal, data.currentMeal),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = contentSize(data.fontScale)
            ),
            maxLines = 3
        )
    }
}

@Composable
private fun MediumLayout(data: WidgetData) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = data.displayDate,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = titleSize(data.fontScale),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        
        MealType.entries.forEach { type ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = labelSize(data.fontScale),
                        fontWeight = if (type == data.currentMeal) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = GlanceModifier.width(26.dp)
                )
                Text(
                    text = getMealContent(data.meal, type),
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = contentSize(data.fontScale)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LargeLayout(data: WidgetData) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = data.displayDate,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = titleSize(data.fontScale),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        
        MealType.entries.forEach { type ->
            val isActive = type == data.currentMeal
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(if (isActive) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(4.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = type.label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = labelSize(data.fontScale),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = getMealContent(data.meal, type),
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = contentSize(data.fontScale)
                    ),
                    maxLines = 3
                )
            }
            if (type != MealType.DINNER) Spacer(modifier = GlanceModifier.height(2.dp))
        }
        
        if (data.showCalories && !data.meal?.sumCal.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "${data.meal?.sumCal} kcal",
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = smallSize(data.fontScale)
                )
            )
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

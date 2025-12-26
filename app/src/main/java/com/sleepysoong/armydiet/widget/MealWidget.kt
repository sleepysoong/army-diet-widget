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
        
        private const val BASE_FONT_TITLE = 11
        private const val BASE_FONT_LABEL = 10
        private const val BASE_FONT_CONTENT = 10
        private const val BASE_FONT_SMALL = 9
        
        private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")
    }

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        
        provideContent {
            GlanceTheme {
                WidgetContent(
                    data = data,
                    size = LocalSize.current
                )
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
        
        val configData = WidgetConfigData(
            fontScale = config.fontScale.first(),
            bgAlpha = config.backgroundAlpha.first(),
            showCalories = config.showCalories.first()
        )
        
        WidgetData(
            meal = meal,
            displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M/d")),
            currentMeal = getCurrentMealType(),
            config = configData
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
    val config: WidgetConfigData
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
            .padding(8.dp)
    ) {
        when (layout) {
            WidgetLayout.SMALL -> SmallLayout(data)
            WidgetLayout.MEDIUM -> MediumLayout(data)
            WidgetLayout.LARGE -> LargeLayout(data)
        }
    }
}

private enum class WidgetLayout { SMALL, MEDIUM, LARGE }

@Composable
private fun SmallLayout(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MealLabel(data.currentMeal.label, isActive = true, data.config)
        Spacer(modifier = GlanceModifier.height(2.dp))
        MealText(getMealContent(data.meal, data.currentMeal), data.config, maxLines = 3)
    }
}

@Composable
private fun MediumLayout(data: WidgetData) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        DateHeader(data.displayDate, data.config)
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        MealType.entries.forEach { type ->
            MealRow(type, getMealContent(data.meal, type), type == data.currentMeal, data.config)
        }
    }
}

@Composable
private fun LargeLayout(data: WidgetData) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        DateHeader(data.displayDate, data.config)
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        MealType.entries.forEach { type ->
            MealCard(type, getMealContent(data.meal, type), type == data.currentMeal, data.config)
            if (type != MealType.DINNER) Spacer(modifier = GlanceModifier.height(2.dp))
        }
        
        if (data.config.showCalories && !data.meal?.sumCal.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            CaloriesText(data.meal?.sumCal ?: "", data.config)
        }
    }
}

@Composable
private fun DateHeader(date: String, config: WidgetConfigData) {
    Text(
        text = date,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = (BASE_FONT_TITLE * config.fontScale).sp,
            fontWeight = FontWeight.Bold
        )
    )
}

private const val BASE_FONT_TITLE = 11
private const val BASE_FONT_LABEL = 10
private const val BASE_FONT_CONTENT = 10
private const val BASE_FONT_SMALL = 9

@Composable
private fun MealLabel(label: String, isActive: Boolean, config: WidgetConfigData) {
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = (BASE_FONT_LABEL * config.fontScale).sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    )
}

@Composable
private fun MealText(text: String, config: WidgetConfigData, maxLines: Int = 1) {
    Text(
        text = text,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = (BASE_FONT_CONTENT * config.fontScale).sp
        ),
        maxLines = maxLines
    )
}

@Composable
private fun MealRow(type: MealType, content: String, isActive: Boolean, config: WidgetConfigData) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type.label,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = (BASE_FONT_LABEL * config.fontScale).sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = GlanceModifier.width(28.dp)
        )
        MealText(content, config)
    }
}

@Composable
private fun MealCard(type: MealType, content: String, isActive: Boolean, config: WidgetConfigData) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(if (isActive) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant)
            .cornerRadius(4.dp)
            .padding(4.dp)
    ) {
        MealLabel(type.label, isActive, config)
        MealText(content, config, maxLines = 3)
    }
}

@Composable
private fun CaloriesText(calories: String, config: WidgetConfigData) {
    Text(
        text = "$calories kcal",
        style = TextStyle(
            color = GlanceTheme.colors.secondary,
            fontSize = (BASE_FONT_SMALL * config.fontScale).sp
        )
    )
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
    return ALLERGY_REGEX.replace(content, "").trim()
}

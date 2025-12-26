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
import androidx.glance.unit.ColorProvider
import com.sleepysoong.armydiet.MainActivity
import com.sleepysoong.armydiet.R
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.MealEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Constants
private object WidgetConstants {
    val SIZE_SMALL = DpSize(100.dp, 48.dp)
    val SIZE_MEDIUM = DpSize(180.dp, 100.dp)
    val SIZE_LARGE = DpSize(250.dp, 180.dp)
    
    const val BASE_FONT_TITLE = 11
    const val BASE_FONT_LABEL = 10
    const val BASE_FONT_CONTENT = 10
    const val BASE_FONT_SMALL = 9
}

private val allergyRegex = Regex("\\([0-9.]+\\)")

class MealWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(WidgetConstants.SIZE_SMALL, WidgetConstants.SIZE_MEDIUM, WidgetConstants.SIZE_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val meal = loadMealData(context)
        val config = loadConfig(context)
        val displayDate = getTargetDate().format(DateTimeFormatter.ofPattern("M/d"))
        val currentMeal = getCurrentMealType()

        provideContent {
            GlanceTheme {
                WidgetContent(
                    meal = meal,
                    displayDate = displayDate,
                    currentMeal = currentMeal,
                    size = LocalSize.current,
                    config = config
                )
            }
        }
    }

    private suspend fun loadMealData(context: Context): MealEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val dateStr = getTargetDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            AppDatabase.getDatabase(context).mealDao().getMeal(dateStr)
        }.getOrNull()
    }

    private suspend fun loadConfig(context: Context): WidgetConfigData = withContext(Dispatchers.IO) {
        val config = WidgetConfig(context)
        WidgetConfigData(
            fontScale = config.fontScale.first(),
            bgAlpha = config.backgroundAlpha.first(),
            showCalories = config.showCalories.first()
        )
    }

    private fun getTargetDate(): LocalDate {
        val now = LocalTime.now()
        return if (now.hour >= 18) LocalDate.now().plusDays(1) else LocalDate.now()
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

// Widget Content - Main Composable
@Composable
private fun WidgetContent(
    meal: MealEntity?,
    displayDate: String,
    currentMeal: MealType,
    size: DpSize,
    config: WidgetConfigData
) {
    val layout = determineLayout(size)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp)
    ) {
        when (layout) {
            WidgetLayout.SMALL -> SmallLayout(meal, currentMeal, config)
            WidgetLayout.MEDIUM -> MediumLayout(meal, displayDate, currentMeal, config)
            WidgetLayout.LARGE -> LargeLayout(meal, displayDate, currentMeal, config)
        }
    }
}

private enum class WidgetLayout { SMALL, MEDIUM, LARGE }

private fun determineLayout(size: DpSize): WidgetLayout = when {
    size.width < 180.dp || size.height < 100.dp -> WidgetLayout.SMALL
    size.width >= 250.dp && size.height >= 180.dp -> WidgetLayout.LARGE
    else -> WidgetLayout.MEDIUM
}

// Small Layout - Current meal only
@Composable
private fun SmallLayout(meal: MealEntity?, currentMeal: MealType, config: WidgetConfigData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MealLabel(currentMeal.label, isActive = true, config = config)
        Spacer(modifier = GlanceModifier.height(2.dp))
        MealText(
            text = getMealContent(meal, currentMeal),
            config = config,
            maxLines = 3
        )
    }
}

// Medium Layout - Date + 3 meals summary
@Composable
private fun MediumLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMeal: MealType,
    config: WidgetConfigData
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        DateHeader(displayDate, config)
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        MealType.entries.forEach { type ->
            MealRow(
                type = type,
                content = getMealContent(meal, type),
                isActive = type == currentMeal,
                config = config
            )
        }
    }
}

// Large Layout - Full detail with calories
@Composable
private fun LargeLayout(
    meal: MealEntity?,
    displayDate: String,
    currentMeal: MealType,
    config: WidgetConfigData
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        DateHeader(displayDate, config)
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        MealType.entries.forEach { type ->
            MealCard(
                type = type,
                content = getMealContent(meal, type),
                isActive = type == currentMeal,
                config = config
            )
            if (type != MealType.DINNER) {
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
        
        if (config.showCalories && !meal?.sumCal.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            CaloriesText(meal?.sumCal ?: "", config)
        }
    }
}

// Reusable Components
@Composable
private fun DateHeader(date: String, config: WidgetConfigData) {
    Text(
        text = date,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = (WidgetConstants.BASE_FONT_TITLE * config.fontScale).sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun MealLabel(label: String, isActive: Boolean, config: WidgetConfigData) {
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.onBackground,
            fontSize = (WidgetConstants.BASE_FONT_LABEL * config.fontScale).sp,
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
            fontSize = (WidgetConstants.BASE_FONT_CONTENT * config.fontScale).sp
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
                fontSize = (WidgetConstants.BASE_FONT_LABEL * config.fontScale).sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = GlanceModifier.width(28.dp)
        )
        MealText(text = content, config = config, maxLines = 1)
    }
}

@Composable
private fun MealCard(type: MealType, content: String, isActive: Boolean, config: WidgetConfigData) {
    val bgColor = if (isActive) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant
    
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(bgColor)
            .cornerRadius(4.dp)
            .padding(4.dp)
    ) {
        MealLabel(type.label, isActive, config)
        MealText(text = content, config = config, maxLines = 3)
    }
}

@Composable
private fun CaloriesText(calories: String, config: WidgetConfigData) {
    Text(
        text = "$calories kcal",
        style = TextStyle(
            color = GlanceTheme.colors.secondary,
            fontSize = (WidgetConstants.BASE_FONT_SMALL * config.fontScale).sp
        )
    )
}

// Utility
private fun getMealContent(meal: MealEntity?, type: MealType): String {
    if (meal == null) return "-"
    val content = when (type) {
        MealType.BREAKFAST -> meal.breakfast
        MealType.LUNCH -> meal.lunch
        MealType.DINNER -> meal.dinner
    }
    if (content.isBlank() || content == "메뉴 정보 없음") return "-"
    return allergyRegex.replace(content, "").trim()
}

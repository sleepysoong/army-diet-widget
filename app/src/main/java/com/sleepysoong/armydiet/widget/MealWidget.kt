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
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.MealEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MealWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(100.dp, 48.dp)
        private val MEDIUM = DpSize(180.dp, 100.dp)
        private val LARGE = DpSize(250.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val meal = loadMealData(context)
        val targetDate = getTargetDate()
        val displayDate = targetDate.format(DateTimeFormatter.ofPattern("M/d"))
        val currentMealType = getCurrentMealType()

        provideContent {
            val size = LocalSize.current
            GlanceTheme {
                WidgetContent(meal, displayDate, currentMealType, size)
            }
        }
    }

    private suspend fun loadMealData(context: Context): MealEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(context)
                val dateStr = getTargetDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                database.mealDao().getMeal(dateStr)
            } catch (e: Exception) { null }
        }
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

enum class MealType(val label: String) {
    BREAKFAST("조식"), LUNCH("중식"), DINNER("석식")
}

@Composable
private fun WidgetContent(meal: MealEntity?, displayDate: String, currentMealType: MealType, size: DpSize) {
    val isSmall = size.width < 180.dp || size.height < 100.dp
    val isLarge = size.width >= 250.dp && size.height >= 180.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp)
    ) {
        when {
            isLarge -> LargeLayout(meal, displayDate, currentMealType)
            isSmall -> SmallLayout(meal, currentMealType)
            else -> MediumLayout(meal, displayDate, currentMealType)
        }
    }
}

@Composable
private fun SmallLayout(meal: MealEntity?, currentMealType: MealType) {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentMealType.label,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = getMealContent(meal, currentMealType),
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 10.sp),
            maxLines = 3
        )
    }
}

@Composable
private fun MediumLayout(meal: MealEntity?, displayDate: String, currentMealType: MealType) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = displayDate,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        MealType.entries.forEach { type ->
            val isActive = type == currentMealType
            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(
                    text = type.label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = GlanceModifier.width(28.dp)
                )
                Text(
                    text = getMealContent(meal, type),
                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 10.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun LargeLayout(meal: MealEntity?, displayDate: String, currentMealType: MealType) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = displayDate,
            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        MealType.entries.forEach { type ->
            val isActive = type == currentMealType
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(if (isActive) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(4.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = type.label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = getMealContent(meal, type),
                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 10.sp),
                    maxLines = 3
                )
            }
        }
        if (!meal?.sumCal.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "${meal?.sumCal} kcal",
                style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 9.sp)
            )
        }
    }
}

private val allergyRegex = Regex("\\([0-9.]+\\)")

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

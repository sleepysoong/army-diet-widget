package com.sleepysoong.armydiet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.ui.components.EmptyState
import com.sleepysoong.armydiet.ui.components.MealCard
import com.sleepysoong.armydiet.ui.theme.ArmyColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

private val ALLERGY_REGEX = Regex("\\([0-9.]+\\)")

@Composable
fun CalendarScreen(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    mealData: Map<String, MealEntity>,
    selectedMeal: MealEntity?,
    keywords: Set<String> = emptySet(),
    modifier: Modifier = Modifier
)
{
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive logic (Foldable/Tablet)
    val isWideScreen = screenWidth > 600.dp
    
    // Shared Calendar Grid Component
    val calendarGrid = @Composable { weight: Float ->
        Column(
            modifier = Modifier
                .weight(weight)
                .fillMaxHeight()
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )
            WeekDayHeader()
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                mealData = mealData,
                onDateSelected = onDateSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Shared Meal Detail Component
    val mealDetail = @Composable { weight: Float ->
        MealDetailView(
            date = selectedDate,
            meal = selectedMeal,
            keywords = keywords,
            modifier = Modifier
                .weight(weight)
                .fillMaxHeight()
        )
    }

    if (isWideScreen) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            calendarGrid(1f)
            mealDetail(1f)
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            calendarGrid(0.45f) // Calendar takes top 45%
            Spacer(modifier = Modifier.height(16.dp))
            mealDetail(0.55f)   // Detail takes bottom 55%
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달", tint = ArmyColors.Primary)
        }
        
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ArmyColors.Primary
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달", tint = ArmyColors.Primary)
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        weekDays.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when (index) {
                    0 -> ArmyColors.Error.copy(alpha = 0.8f) // Sunday
                    6 -> Color(0xFF1976D2).copy(alpha = 0.8f) // Saturday
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    mealData: Map<String, MealEntity>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
)
{
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    val totalDays = firstDayOfWeek + lastDayOfMonth.dayOfMonth
    val days = (0 until totalDays).map { index ->
        if (index < firstDayOfWeek) null
        else firstDayOfMonth.plusDays((index - firstDayOfWeek).toLong())
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(days) { date ->
            if (date != null) {
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == LocalDate.now(),
                    hasMeal = mealData.containsKey(date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))),
                    onClick = { onDateSelected(date) }
                )
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasMeal: Boolean,
    onClick: () -> Unit
)
{
    val dayOfWeek = date.dayOfWeek.value % 7
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> ArmyColors.Primary
                    isToday -> ArmyColors.PrimaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> ArmyColors.OnPrimary
                    dayOfWeek == 0 -> ArmyColors.Error
                    dayOfWeek == 6 -> Color(0xFF1976D2)
                    else -> ArmyColors.OnSurface
                }
            )
            if (hasMeal) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) ArmyColors.OnPrimary
                            else ArmyColors.PrimaryLight
                        )
                )
            }
        }
    }
}

@Composable
private fun MealDetailView(
    date: LocalDate,
    meal: MealEntity?,
    keywords: Set<String>,
    modifier: Modifier = Modifier
)
{
    val displayDate = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ArmyColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ArmyColors.Primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (meal == null) {
                EmptyState(message = "선택한 날짜의 식단이 없습니다.")
            } else {
                MealCard("아침", cleanAllergyInfo(meal.breakfast), keywords)
                MealCard("점심", cleanAllergyInfo(meal.lunch), keywords)
                MealCard("저녁", cleanAllergyInfo(meal.dinner), keywords)
                
                formatCalories(meal.sumCal)?.let { cal ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "총 칼로리: $cal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

private fun cleanAllergyInfo(text: String): String {
    if (text.isBlank()) return ""
    return ALLERGY_REGEX.replace(text, "").replace("  ", " ").trim()
}

private fun formatCalories(sumCal: String?): String? {
    if (sumCal.isNullOrBlank()) return null
    val cleaned = sumCal.replace("kcal", "").replace("Kcal", "").replace("KCAL", "").trim()
    val value = cleaned.toDoubleOrNull() ?: return null
    return "${value.toInt()} kcal"
}

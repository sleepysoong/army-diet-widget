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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepysoong.armydiet.data.local.MealEntity
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
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // 폴드/태블릿 대응: 가로가 넓으면 가로 배치
    val isWideScreen = screenWidth > 600.dp
    
    if (isWideScreen) {
        // 가로 레이아웃 (폴드 펼침, 태블릿)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            // 캘린더 (왼쪽)
            Column(
                modifier = Modifier
                    .weight(1f)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 식단 정보 (오른쪽)
            SelectedDateMeal(
                date = selectedDate,
                meal = selectedMeal,
                keywords = keywords,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        // 세로 레이아웃 (일반 폰, 폴드 접힘)
        Column(modifier = modifier.fillMaxSize()) {
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
                modifier = Modifier.weight(0.45f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SelectedDateMeal(
                date = selectedDate,
                meal = selectedMeal,
                keywords = keywords,
                modifier = Modifier.weight(0.55f)
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "이전 달")
        }
        
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        weekDays.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = when (index) {
                    0 -> Color.Red.copy(alpha = 0.7f)
                    6 -> Color.Blue.copy(alpha = 0.7f)
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
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    val days = mutableListOf<LocalDate?>()
    
    repeat(firstDayOfWeek) { days.add(null) }
    
    var currentDay = firstDayOfMonth
    while (!currentDay.isAfter(lastDayOfMonth)) {
        days.add(currentDay)
        currentDay = currentDay.plusDays(1)
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        userScrollEnabled = false
    ) {
        items(days) { date ->
            if (date != null) {
                val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                val hasMeal = mealData.containsKey(dateStr)
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                
                DayCell(
                    date = date,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasMeal = hasMeal,
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
) {
    val dayOfWeek = date.dayOfWeek.value % 7
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    dayOfWeek == 0 -> Color.Red.copy(alpha = 0.8f)
                    dayOfWeek == 6 -> Color.Blue.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasMeal) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedDateMeal(
    date: LocalDate,
    meal: MealEntity?,
    keywords: Set<String>,
    modifier: Modifier = Modifier
) {
    val displayDate = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = displayDate,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (meal == null) {
                Text(
                    text = "식단 정보 없음",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                MealItem("조식", cleanAllergyInfo(meal.breakfast), keywords)
                MealItem("중식", cleanAllergyInfo(meal.lunch), keywords)
                MealItem("석식", cleanAllergyInfo(meal.dinner), keywords)
                
                formatCalories(meal.sumCal)?.let { cal ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MealItem(title: String, content: String, keywords: Set<String>) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        val displayText = if (content.isBlank() || content == "메뉴 정보 없음") "-" else content
        val annotatedString = buildAnnotatedString {
            append(displayText)
            keywords.forEach { keyword ->
                var startIndex = displayText.indexOf(keyword)
                while (startIndex >= 0) {
                    val endIndex = startIndex + keyword.length
                    addStyle(
                        style = SpanStyle(
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold
                        ),
                        start = startIndex,
                        end = endIndex
                    )
                    startIndex = displayText.indexOf(keyword, endIndex)
                }
            }
        }
        
        Text(
            text = annotatedString,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
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

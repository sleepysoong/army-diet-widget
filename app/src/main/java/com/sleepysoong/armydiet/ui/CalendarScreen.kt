package com.sleepysoong.armydiet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.ui.components.EmptyState
import com.sleepysoong.armydiet.ui.components.MealCard
import com.sleepysoong.armydiet.ui.theme.ArmyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    onMealEdit: suspend (MealEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth > 600.dp
    
    if (isWideScreen) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CalendarSection(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                mealData = mealData,
                onDateSelected = onDateSelected,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MealDetailSection(
                selectedDate = selectedDate,
                selectedMeal = selectedMeal,
                keywords = keywords,
                onMealEdit = onMealEdit,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            CalendarSection(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                mealData = mealData,
                onDateSelected = onDateSelected,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                modifier = Modifier.weight(0.45f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            MealDetailSection(
                selectedDate = selectedDate,
                selectedMeal = selectedMeal,
                keywords = keywords,
                onMealEdit = onMealEdit,
                modifier = Modifier.weight(0.55f)
            )
        }
    }
}

@Composable
private fun CalendarSection(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    mealData: Map<String, MealEntity>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
            Spacer(modifier = Modifier.height(16.dp))
            WeekDayHeader()
            Spacer(modifier = Modifier.height(8.dp))
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                mealData = mealData,
                onDateSelected = onDateSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MealDetailSection(
    selectedDate: LocalDate,
    selectedMeal: MealEntity?,
    keywords: Set<String>,
    onMealEdit: suspend (MealEntity) -> Unit,
    modifier: Modifier
) {
    MealDetailView(
        date = selectedDate,
        meal = selectedMeal,
        keywords = keywords,
        onMealEdit = onMealEdit,
        modifier = modifier
    )
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
    
    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = when (index) {
                    0 -> ArmyColors.Error
                    6 -> Color(0xFF007AFF) // iOS Blue
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
    
    val totalDays = firstDayOfWeek + lastDayOfMonth.dayOfMonth
    val days = (0 until totalDays).map { index ->
        if (index < firstDayOfWeek) null
        else firstDayOfMonth.plusDays((index - firstDayOfWeek).toLong())
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
) {
    val dayOfWeek = date.dayOfWeek.value % 7
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> ArmyColors.Primary
            isToday -> ArmyColors.Primary.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "day_bg"
    )
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    dayOfWeek == 0 -> ArmyColors.Error
                    dayOfWeek == 6 -> Color(0xFF007AFF)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasMeal && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(ArmyColors.Primary)
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
    onMealEdit: suspend (MealEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDate = date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    var showEditDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "수정",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (meal == null) {
                EmptyState(message = "식단 정보가 없습니다")
            } else {
                MealCard("아침", cleanAllergyInfo(meal.breakfast), keywords)
                Spacer(modifier = Modifier.height(8.dp))
                MealCard("점심", cleanAllergyInfo(meal.lunch), keywords)
                Spacer(modifier = Modifier.height(8.dp))
                MealCard("저녁", cleanAllergyInfo(meal.dinner), keywords)
                
                val calories = formatCalories(meal.sumCal)
                if (calories != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = calories,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
    
    if (showEditDialog) {
        MealEditDialog(
            date = dateStr,
            meal = meal,
            onDismiss = { showEditDialog = false },
            onSave = { editedMeal ->
                coroutineScope.launch(Dispatchers.IO) {
                    onMealEdit(editedMeal)
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun MealEditDialog(
    date: String,
    meal: MealEntity?,
    onDismiss: () -> Unit,
    onSave: (MealEntity) -> Unit
) {
    var breakfast by remember { mutableStateOf(meal?.breakfast ?: "") }
    var lunch by remember { mutableStateOf(meal?.lunch ?: "") }
    var dinner by remember { mutableStateOf(meal?.dinner ?: "") }
    var calories by remember { mutableStateOf(meal?.sumCal ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "식단 수정",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = breakfast,
                    onValueChange = { breakfast = it },
                    label = { Text("아침") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                OutlinedTextField(
                    value = lunch,
                    onValueChange = { lunch = it },
                    label = { Text("점심") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                OutlinedTextField(
                    value = dinner,
                    onValueChange = { dinner = it },
                    label = { Text("저녁") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("칼로리 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    MealEntity(
                        date = date,
                        breakfast = breakfast,
                        lunch = lunch,
                        dinner = dinner,
                        adspcfd = meal?.adspcfd ?: "",
                        sumCal = calories
                    )
                )
            }) {
                Text("저장", color = ArmyColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
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

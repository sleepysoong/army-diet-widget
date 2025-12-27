package com.sleepysoong.armydiet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.di.AppContainer
import com.sleepysoong.armydiet.ui.CalendarScreen
import com.sleepysoong.armydiet.ui.MainViewModel
import com.sleepysoong.armydiet.ui.MainViewModelFactory
import com.sleepysoong.armydiet.ui.MealUiState
import com.sleepysoong.armydiet.ui.components.EmptyState
import com.sleepysoong.armydiet.ui.components.ErrorState
import com.sleepysoong.armydiet.ui.components.LoadingState
import com.sleepysoong.armydiet.ui.components.MealCard
import com.sleepysoong.armydiet.ui.theme.AppTheme
import com.sleepysoong.armydiet.ui.theme.ArmyColors
import com.sleepysoong.armydiet.worker.SyncWorker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private val container: AppContainer by lazy {
        (application as ArmyDietApp).container
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        scheduleSyncWorker()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val factory = MainViewModelFactory(
                        container.mealRepository,
                        container.preferences,
                        applicationContext
                    )
                    val viewModel: MainViewModel = viewModel(factory = factory)
                    MainScreen(viewModel, container)
                }
            }
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, container: AppContainer) {
    var currentTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keywords by container.preferences.highlightKeywords.collectAsStateWithLifecycle(initialValue = emptySet())
    val context = LocalContext.current
    
    // 캘린더용 상태
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var allMeals by remember { mutableStateOf<Map<String, MealEntity>>(emptyMap()) }
    var selectedMeal by remember { mutableStateOf<MealEntity?>(null) }
    
    // 식단 데이터 로드
    LaunchedEffect(Unit) {
        container.mealDao.getAllMealsFlow().collect { mealList ->
            allMeals = mealList.associateBy { it.date }
        }
    }
    
    // 선택된 날짜의 식단 업데이트
    LaunchedEffect(selectedDate, allMeals) {
        val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        selectedMeal = allMeals[dateStr]
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "군대 식단",
                        fontWeight = FontWeight.Bold,
                        color = ArmyColors.Primary
                    ) 
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정", tint = ArmyColors.OnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArmyColors.Surface,
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ArmyColors.Surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "오늘") },
                    label = { Text("오늘") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ArmyColors.Primary,
                        selectedTextColor = ArmyColors.Primary,
                        indicatorColor = ArmyColors.PrimaryContainer
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "캘린더") },
                    label = { Text("캘린더") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ArmyColors.Primary,
                        selectedTextColor = ArmyColors.Primary,
                        indicatorColor = ArmyColors.PrimaryContainer
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                0 -> TodayScreen(uiState, viewModel, keywords)
                1 -> CalendarScreen(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    mealData = allMeals,
                    selectedMeal = selectedMeal,
                    keywords = keywords,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TodayScreen(uiState: MealUiState, viewModel: MainViewModel, keywords: Set<String>) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is MealUiState.ApiKeyMissing -> ApiKeyInputScreen(viewModel::saveApiKey)
            is MealUiState.Loading -> LoadingState()
            is MealUiState.Error -> ErrorState(uiState.message, viewModel::loadMeal, viewModel::resetApiKey)
            is MealUiState.Success -> MealContent(uiState, viewModel, keywords)
        }
    }
}

@Composable
fun ApiKeyInputScreen(onKeyEntered: (String) -> Unit) {
    // Basic implementation, consider refactoring to separate file if complex
    var apiKey by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "API Key 필요",
            style = MaterialTheme.typography.headlineMedium,
            color = ArmyColors.Primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "국방부 공공데이터 포털에서 발급받은\nAPI Key를 입력해주세요.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (apiKey.isNotBlank()) {
                    onKeyEntered(apiKey.trim())
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("시작하기")
        }
    }
}

@Composable
fun MealContent(state: MealUiState.Success, viewModel: MainViewModel, keywords: Set<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Date Header
        Text(
            text = state.targetDate,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ArmyColors.Primary
        )
        
        formatCalories(state.meal?.sumCal)?.let { cal ->
            Text(
                text = "총 칼로리: $cal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (state.meal == null) {
            EmptyState(message = "오늘의 식단 정보가 없습니다.")
        } else {
            MealCard("아침", state.meal.breakfast, keywords)
            MealCard("점심", state.meal.lunch, keywords)
            MealCard("저녁", state.meal.dinner, keywords)
            
            if (state.meal.adspcfd.isNotBlank() && state.meal.adspcfd != "메뉴 정보 없음") {
                MealCard("부식", state.meal.adspcfd, keywords)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = viewModel::loadMeal,
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text("새로고침") 
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatCalories(sumCal: String?): String? {
    if (sumCal.isNullOrBlank()) return null
    val cleaned = sumCal.replace("kcal", "").replace("Kcal", "").replace("KCAL", "").trim()
    val value = cleaned.toDoubleOrNull() ?: return null
    return "${value.toInt()} kcal"
}
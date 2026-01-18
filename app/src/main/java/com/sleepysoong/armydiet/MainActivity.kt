package com.sleepysoong.armydiet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.sleepysoong.armydiet.data.local.AppPreferences
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
        enableEdgeToEdge()
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
        container.mealDao.getAllMealsFlow().collect {
            allMeals = it.associateBy { meal -> meal.date }
        }
    }

    // 선택된 날짜의 식단 업데이트
    LaunchedEffect(selectedDate, allMeals) {
        val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        selectedMeal = allMeals[dateStr]
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Header
            AppleHeader(
                title = "🍚 군대 식단",
                onSettingsClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            )

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 100.dp) // Space for floating bar
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                                fadeOut(animationSpec = tween(200))
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        0 -> TodayScreen(uiState, viewModel, keywords)
                        1 -> CalendarScreen(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            mealData = allMeals,
                            selectedMeal = selectedMeal,
                            keywords = keywords,
                            onMealEdit = { meal -> container.mealDao.insertMeals(listOf(meal)) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }

        // Floating Navigation Bar
        FloatingNavBar(
            selectedIndex = currentTab,
            onItemSelected = { currentTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun AppleHeader(
    title: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "설정",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FloatingNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Icons.Rounded.Home to "홈",
        Icons.Rounded.DateRange to "캘린더"
    )

    Row(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, (icon, _) ->
            FloatingNavItem(
                icon = icon,
                isSelected = selectedIndex == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ArmyColors.Primary else Color.Transparent,
        animationSpec = tween(200),
        label = "nav_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "nav_icon"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun TodayScreen(uiState: MealUiState, viewModel: MainViewModel, keywords: Set<String>) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp.dp > 600.dp

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is MealUiState.SourceSelection -> SourceSelectionScreen(viewModel::selectSource)
            is MealUiState.ApiKeyMissing -> ApiKeyInputScreen(viewModel::saveApiKey)
            is MealUiState.ExternalEndpointMissing -> ExternalEndpointInputScreen(viewModel::saveExternalEndpoint)
            is MealUiState.Loading -> LoadingState()
            is MealUiState.Error -> ErrorState(
                message = uiState.message,
                onRetry = viewModel::loadMeal,
                onReset = { viewModel.resetForError(uiState.isExternalSource) },
                resetLabel = if (uiState.isExternalSource) "외부 API 재설정" else "API Key 재설정"
            )
            is MealUiState.Success -> MealContent(uiState, viewModel, keywords, isWideScreen)
        }
    }
}

@Composable
fun SourceSelectionScreen(onSourceSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "시작하기",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "식단 데이터를 어디서 가져올까요?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        // 국방부 API 선택
        AppleOptionCard(
            title = "국방부 공공데이터",
            subtitle = "API Key 필요",
            onClick = { onSourceSelected(AppPreferences.SOURCE_LOCAL) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 외부 API 선택
        AppleOptionCard(
            title = "외부 API 서버",
            subtitle = "직접 구축한 서버 사용",
            onClick = { onSourceSelected(AppPreferences.SOURCE_EXTERNAL) }
        )
    }
}

@Composable
private fun AppleOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ApiKeyInputScreen(onKeyEntered: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "API Key 입력",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "공공데이터포털에서 발급받은\nAPI Key를 입력해주세요",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            placeholder = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = ArmyColors.Primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (apiKey.isNotBlank()) {
                    onKeyEntered(apiKey.trim())
                    focusManager.clearFocus()
                }
            })
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (apiKey.isNotBlank()) {
                    onKeyEntered(apiKey.trim())
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = apiKey.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ArmyColors.Primary
            )
        ) {
            Text(
                "계속",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExternalEndpointInputScreen(onEndpointEntered: (String) -> Unit) {
    var endpoint by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "서버 주소 입력",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "외부 API 서버 주소를 입력해주세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            placeholder = { Text("https://example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = ArmyColors.Primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (endpoint.isNotBlank()) {
                    onEndpointEntered(endpoint.trim())
                    focusManager.clearFocus()
                }
            })
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (endpoint.isNotBlank()) {
                    onEndpointEntered(endpoint.trim())
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = endpoint.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ArmyColors.Primary
            )
        ) {
            Text(
                "계속",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealContent(
    state: MealUiState.Success, 
    viewModel: MainViewModel, 
    keywords: Set<String>,
    isWideScreen: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Date Header - Apple Style Large Title
        Text(
            text = state.targetDate,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        val calories = formatCalories(state.meal?.sumCal)
        if (calories != null) {
            Text(
                text = calories,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (state.meal == null) {
            EmptyState(message = "식단 정보가 없습니다")
        } else {
            val meals = listOfNotNull(
                "아침" to state.meal.breakfast,
                "점심" to state.meal.lunch,
                "저녁" to state.meal.dinner,
                if (state.meal.adspcfd.isNotBlank() && state.meal.adspcfd != "메뉴 정보 없음") 
                    "부식" to state.meal.adspcfd else null
            )

            if (isWideScreen) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    meals.forEach { (title, content) ->
                        MealCard(
                            title = title,
                            content = content,
                            keywords = keywords,
                            modifier = Modifier.fillMaxWidth(0.48f)
                        )
                    }
                }
            } else {
                meals.forEach { (title, content) ->
                    MealCard(title, content, keywords)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Refresh Button - Minimal Style
        TextButton(
            onClick = viewModel::loadMeal,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { 
            Text(
                "새로고침",
                color = ArmyColors.Primary,
                style = MaterialTheme.typography.bodyLarge
            ) 
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatCalories(sumCal: String?): String? {
    if (sumCal.isNullOrBlank()) return null
    val cleaned = sumCal.replace("kcal", "").replace("Kcal", "").replace("KCAL", "").trim()
    val value = cleaned.toDoubleOrNull() ?: return null
    return "${value.toInt()} kcal"
}

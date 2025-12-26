package com.sleepysoong.armydiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.sleepysoong.armydiet.data.local.AppDatabase
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.remote.NetworkModule
import com.sleepysoong.armydiet.domain.MealRepository
import com.sleepysoong.armydiet.ui.MainViewModel
import com.sleepysoong.armydiet.ui.MainViewModelFactory
import com.sleepysoong.armydiet.ui.MealUiState
import com.sleepysoong.armydiet.ui.theme.ArmyDietTheme
import com.sleepysoong.armydiet.worker.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val preferences = AppPreferences(applicationContext)
        val repository = MealRepository(database.mealDao(), NetworkModule.api, preferences)
        val viewModelFactory = MainViewModelFactory(repository, preferences, applicationContext)

        setupWorker()

        setContent {
            ArmyDietTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                    MealScreen(viewModel)
                }
            }
        }
    }

    private fun setupWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_meal_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}

@Composable
fun MealScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogs by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is MealUiState.ApiKeyMissing -> {
                    ApiKeyInputScreen(onKeyEntered = { viewModel.saveApiKey(it) })
                }
                is MealUiState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MealUiState.Error -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMeal() }) {
                            Text("다시 시도")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.resetApiKey() }) {
                            Text("API Key 재설정")
                        }
                    }
                }
                is MealUiState.Success -> {
                    MealContent(state, viewModel)
                }
            }
        }

        TextButton(
            onClick = { showLogs = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("LOGS")
        }

        if (showLogs) {
            LogViewerDialog(
                viewModel = viewModel,
                onDismiss = { showLogs = false }
            )
        }
    }
}

@Composable
fun LogViewerDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val logs by viewModel.debugLogs.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug Logs") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFEEEEEE))
                        .padding(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Divider(color = Color.LightGray)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { viewModel.clearLogs() }) {
                    Text("초기화")
                }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Debug Logs", logs.joinToString("\n"))
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "로그가 복사되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("복사")
                }
            }
        }
    )
}

@Composable
fun ApiKeyInputScreen(onKeyEntered: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔑 API Key 입력",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "국방부 공공데이터 포털에서 발급받은\nAPI Key를 입력해주세요.",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank()
        ) {
            Text("저장하고 시작하기")
        }
    }
}

@Composable
fun MealContent(state: MealUiState.Success, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "오늘 먹을 짬",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = state.targetDate,
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (state.meal == null) {
            Text("해당 날짜의 식단 정보가 없어요 ㅠㅠ", textAlign = TextAlign.Center)
        } else {
            MealCard("조식 🌅", state.meal.breakfast)
            Spacer(modifier = Modifier.height(16.dp))
            MealCard("중식 ☀️", state.meal.lunch)
            Spacer(modifier = Modifier.height(16.dp))
            MealCard("석식 🌙", state.meal.dinner)
            
            // 부식 및 칼로리 정보 (있을 경우에만)
            if (state.meal.adspcfd.isNotBlank() && state.meal.adspcfd != "메뉴 정보 없음") {
                Spacer(modifier = Modifier.height(16.dp))
                MealCard("부식 🥛", state.meal.adspcfd)
            }
            if (state.meal.sumCal.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("총 칼로리: ${state.meal.sumCal} kcal", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Button(onClick = { viewModel.loadMeal() }) {
                Text("새로고침")
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(onClick = { viewModel.resetApiKey() }) {
                Text("키 재설정")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MealCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = content,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
    }
}

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
            MaterialTheme {
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        when (val state = uiState) {
            is MealUiState.ApiKeyMissing -> ApiKeyInputScreen(onKeyEntered = { viewModel.saveApiKey(it) })
            is MealUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MealUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadMeal() }) { Text("다시 시도") }
                    TextButton(onClick = { viewModel.resetApiKey() }) { Text("API Key 재설정") }
                }
            }
            is MealUiState.Success -> MealContent(state, viewModel)
        }
    }
}

@Composable
fun ApiKeyInputScreen(onKeyEntered: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
        Text(text = "API Key 입력", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "국방부 공공데이터 포털에서 발급받은 API Key를 입력하세요.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
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
            Text("저장")
        }
    }
}

@Composable
fun MealContent(state: MealUiState.Success, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text = state.targetDate, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (state.meal == null) {
            Text("식단 정보 없음")
        } else {
            MealSection("조식", state.meal.breakfast)
            MealSection("중식", state.meal.lunch)
            MealSection("석식", state.meal.dinner)
            if (state.meal.adspcfd.isNotBlank() && state.meal.adspcfd != "메뉴 정보 없음") {
                MealSection("부식", state.meal.adspcfd)
            }
            if (state.meal.sumCal.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${state.meal.sumCal} kcal", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Button(onClick = { viewModel.loadMeal() }) { Text("새로고침") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { viewModel.resetApiKey() }) { Text("키 재설정") }
        }
    }
}

@Composable
fun MealSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = content, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

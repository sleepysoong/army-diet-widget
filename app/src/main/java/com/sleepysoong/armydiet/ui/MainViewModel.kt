package com.sleepysoong.armydiet.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.domain.MealRepository
import com.sleepysoong.armydiet.util.DebugLogger
import com.sleepysoong.armydiet.widget.MealWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed class MealUiState {
    object Loading : MealUiState()
    object ApiKeyMissing : MealUiState()
    data class Success(val meal: MealEntity?, val targetDate: String) : MealUiState()
    data class Error(val message: String) : MealUiState()
}

class MainViewModel(
    private val repository: MealRepository,
    private val preferences: AppPreferences,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    val debugLogs = DebugLogger.logs

    private var currentApiKey: String? = null

    init {
        checkApiKeyAndLoad()
    }

    private fun checkApiKeyAndLoad() {
        viewModelScope.launch {
            val key = preferences.apiKey.first()
            if (key.isNullOrBlank()) {
                _uiState.value = MealUiState.ApiKeyMissing
            } else {
                currentApiKey = key
                loadMeal()
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferences.saveApiKey(key)
            // 키 변경 시 전체 리셋 (새 키로 처음부터)
            preferences.updateSyncStatus(0, 0)
            currentApiKey = key
            loadMeal()
        }
    }

    fun loadMeal() {
        viewModelScope.launch {
            val key = currentApiKey
            if (key.isNullOrBlank()) {
                _uiState.value = MealUiState.ApiKeyMissing
                return@launch
            }

            _uiState.value = MealUiState.Loading
            DebugLogger.log("VM", "Loading...")
            
            val nowTime = LocalTime.now()
            val nowDate = LocalDate.now()
            
            val targetDate = if (nowTime.hour >= 18) {
                nowDate.plusDays(1)
            } else {
                nowDate
            }

            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val dateStr = targetDate.format(formatter)
            val displayDate = targetDate.format(DateTimeFormatter.ofPattern("M월 d일 (E)"))
            DebugLogger.log("VM", "Target: $dateStr")

            repository.getMeal(dateStr).onSuccess { meal ->
                if (meal != null) {
                    _uiState.value = MealUiState.Success(meal, displayDate)
                    updateWidget()
                } else {
                    DebugLogger.log("VM", "Local miss, triggering load...")
                    try {
                        // DB에 없으면 리셋 여부 확인
                        // lastCheckedIndex가 0이면 reset=true
                        val lastIdx = preferences.lastCheckedIndex.first()
                        val reset = lastIdx == 0
                        
                        repository.load(key, reset)
                        
                        // 다시 조회
                        repository.getMeal(dateStr).onSuccess { newMeal ->
                            _uiState.value = MealUiState.Success(newMeal, displayDate)
                            updateWidget()
                        }.onFailure {
                            _uiState.value = MealUiState.Error("동기화 후에도 데이터가 없습니다.")
                        }
                    } catch (e: Exception) {
                        _uiState.value = MealUiState.Error("동기화 실패: ${e.message}")
                    }
                }
            }.onFailure { e ->
                _uiState.value = MealUiState.Error("DB 조회 실패: ${e.message}")
            }
        }
    }
    
    private fun updateWidget() {
        MealWidgetReceiver.updateAllWidgets(appContext)
    }
    
    fun resetApiKey() {
        viewModelScope.launch {
            preferences.saveApiKey("")
            preferences.updateSyncStatus(0, 0)
            currentApiKey = null
            _uiState.value = MealUiState.ApiKeyMissing
        }
    }
    
    fun clearLogs() {
        DebugLogger.clear()
    }
}

class MainViewModelFactory(
    private val repository: MealRepository,
    private val preferences: AppPreferences,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, preferences, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

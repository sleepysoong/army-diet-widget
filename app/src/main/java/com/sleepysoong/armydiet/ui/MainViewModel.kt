package com.sleepysoong.armydiet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.domain.MealRepository
import com.sleepysoong.armydiet.util.DebugLogger
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
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    val debugLogs = DebugLogger.logs // UI에 노출

    private var currentApiKey: String? = null

    init {
        checkApiKeyAndLoad()
    }

    private fun checkApiKeyAndLoad() {
        viewModelScope.launch {
            val key = preferences.apiKey.first()
            if (key.isNullOrBlank()) {
                _uiState.value = MealUiState.ApiKeyMissing
                DebugLogger.log("VM", "Key missing")
            } else {
                currentApiKey = key
                DebugLogger.log("VM", "Key found, loading...")
                loadMeal()
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            DebugLogger.log("VM", "Saving new key")
            preferences.saveApiKey(key)
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
            DebugLogger.log("VM", "Loading meals...")
            
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
            DebugLogger.log("VM", "Target date: $dateStr")

            repository.getMeal(dateStr).onSuccess { meal ->
                if (meal != null) {
                    _uiState.value = MealUiState.Success(meal, displayDate)
                    DebugLogger.log("VM", "Found local meal")
                } else {
                    DebugLogger.log("VM", "Local miss, syncing...")
                    try {
                        repository.syncRecentData(key)
                        repository.getMeal(dateStr).onSuccess { newMeal ->
                            _uiState.value = MealUiState.Success(newMeal, displayDate)
                            DebugLogger.log("VM", "Sync success, meal found? ${newMeal != null}")
                        }.onFailure {
                            _uiState.value = MealUiState.Error("데이터 동기화 후 조회 실패")
                            DebugLogger.log("VM", "Post-sync lookup fail")
                        }
                    } catch (e: Exception) {
                        _uiState.value = MealUiState.Error("동기화 실패: ${e.message}\n(API Key를 확인해주세요)")
                        DebugLogger.log("VM", "Sync fail: ${e.message}")
                    }
                }
            }.onFailure { e ->
                _uiState.value = MealUiState.Error("로컬 데이터 조회 실패: ${e.message}")
                DebugLogger.log("VM", "Local DB error: ${e.message}")
            }
        }
    }
    
    fun resetApiKey() {
        viewModelScope.launch {
            DebugLogger.log("VM", "Resetting key")
            preferences.saveApiKey("")
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
    private val preferences: AppPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.sleepysoong.armydiet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.domain.MealRepository
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
    object ApiKeyMissing : MealUiState() // 키 입력 필요
    data class Success(val meal: MealEntity?, val targetDate: String) : MealUiState()
    data class Error(val message: String) : MealUiState()
}

class MainViewModel(
    private val repository: MealRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

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

            // 1. DB 조회
            repository.getMeal(dateStr).onSuccess { meal ->
                if (meal != null) {
                    _uiState.value = MealUiState.Success(meal, displayDate)
                } else {
                    // 2. 없으면 동기화 시도
                    try {
                        repository.syncRecentData(key)
                        // 다시 조회
                        repository.getMeal(dateStr).onSuccess { newMeal ->
                            _uiState.value = MealUiState.Success(newMeal, displayDate)
                        }.onFailure { 
                            _uiState.value = MealUiState.Error("데이터 동기화 후 조회 실패")
                        }
                    } catch (e: Exception) {
                        _uiState.value = MealUiState.Error("동기화 실패: ${e.message}\nAPI Key를 확인해주세요.")
                    }
                }
            }.onFailure { e ->
                _uiState.value = MealUiState.Error("로컬 데이터 조회 실패: ${e.message}")
            }
        }
    }
    
    fun resetApiKey() {
        viewModelScope.launch {
            preferences.saveApiKey("")
            currentApiKey = null
            _uiState.value = MealUiState.ApiKeyMissing
        }
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
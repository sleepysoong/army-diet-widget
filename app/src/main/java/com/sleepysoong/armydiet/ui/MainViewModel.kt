package com.sleepysoong.armydiet.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.data.local.MealEntity
import com.sleepysoong.armydiet.domain.MealRepository
import com.sleepysoong.armydiet.widget.MealWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed interface MealUiState {
    data object Loading : MealUiState
    data object ApiKeyMissing : MealUiState
    data class Success(val meal: MealEntity?, val targetDate: String) : MealUiState
    data class Error(val message: String) : MealUiState
}

class MainViewModel(
    private val repository: MealRepository,
    private val preferences: AppPreferences,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    private var cachedApiKey: String? = null

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val key = preferences.apiKey.first()
            if (key.isNullOrBlank()) {
                _uiState.value = MealUiState.ApiKeyMissing
            } else {
                cachedApiKey = key
                loadMeal()
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferences.saveApiKey(key)
            preferences.updateSyncStatus(0, 0)
            cachedApiKey = key
            loadMeal()
        }
    }

    fun loadMeal() {
        val key = cachedApiKey
        if (key.isNullOrBlank()) {
            _uiState.value = MealUiState.ApiKeyMissing
            return
        }

        viewModelScope.launch {
            _uiState.value = MealUiState.Loading
            
            val (dateStr, displayDate) = getTargetDateInfo()
            
            repository.getMeal(dateStr)
                .onSuccess { meal ->
                    if (meal != null) {
                        _uiState.value = MealUiState.Success(meal, displayDate)
                        updateWidget()
                    } else {
                        syncAndRetry(key, dateStr, displayDate)
                    }
                }
                .onFailure { e ->
                    _uiState.value = MealUiState.Error("데이터 로드 실패: ${e.localizedMessage}")
                }
        }
    }
    
    private suspend fun syncAndRetry(key: String, dateStr: String, displayDate: String) {
        val isFirstSync = preferences.lastCheckedIndex.first() == 0
        
        repository.syncIfNeeded(key, forceReset = isFirstSync)
            .onSuccess {
                repository.getMeal(dateStr)
                    .onSuccess { meal ->
                        _uiState.value = MealUiState.Success(meal, displayDate)
                        updateWidget()
                    }
                    .onFailure {
                        _uiState.value = MealUiState.Success(null, displayDate)
                    }
            }
            .onFailure { e ->
                _uiState.value = MealUiState.Error("동기화 실패: ${e.localizedMessage}")
            }
    }
    
    private fun getTargetDateInfo(): Pair<String, String> {
        val now = LocalTime.now()
        val targetDate = if (now.hour >= 18) LocalDate.now().plusDays(1) else LocalDate.now()
        val dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val displayDate = targetDate.format(DateTimeFormatter.ofPattern("M월 d일 (E)"))
        return dateStr to displayDate
    }
    
    private fun updateWidget() {
        MealWidgetReceiver.updateAllWidgets(appContext)
    }

    fun resetApiKey() {
        viewModelScope.launch {
            preferences.saveApiKey("")
            preferences.updateSyncStatus(0, 0)
            cachedApiKey = null
            _uiState.value = MealUiState.ApiKeyMissing
        }
    }
}

class MainViewModelFactory(
    private val repository: MealRepository,
    private val preferences: AppPreferences,
    private val appContext: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MainViewModel(repository, preferences, appContext) as T
    }
}

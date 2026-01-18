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
    data object SourceSelection : MealUiState  // 첫 실행시 소스 선택
    data object ApiKeyMissing : MealUiState
    data object ExternalEndpointMissing : MealUiState
    data class Success(val meal: MealEntity?, val targetDate: String) : MealUiState
    data class Error(val message: String, val isExternalSource: Boolean = false) : MealUiState
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
        loadMeal()
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferences.saveApiKey(key)
            preferences.updateSyncStatus(0, 0)
            cachedApiKey = key
            loadMeal()
        }
    }

    fun saveExternalEndpoint(endpoint: String) {
        viewModelScope.launch {
            preferences.setExternalApiEndpoint(endpoint)
            preferences.setMealSource(AppPreferences.SOURCE_EXTERNAL)
            loadMeal()
        }
    }

    fun selectSource(source: String) {
        viewModelScope.launch {
            preferences.setMealSource(source)
            if (source == AppPreferences.SOURCE_EXTERNAL) {
                _uiState.value = MealUiState.ExternalEndpointMissing
            } else {
                _uiState.value = MealUiState.ApiKeyMissing
            }
        }
    }

    fun loadMeal() {
        viewModelScope.launch {
            val source = preferences.mealSource.first()
            val apiKey = preferences.apiKey.first()
            val endpoint = preferences.externalApiEndpoint.first()
            
            // 첫 실행: 둘 다 없으면 소스 선택 화면
            if (apiKey.isNullOrBlank() && endpoint.isNullOrBlank()) {
                _uiState.value = MealUiState.SourceSelection
                return@launch
            }
            
            // 소스별 필수값 체크
            if (source == AppPreferences.SOURCE_EXTERNAL) {
                if (endpoint.isNullOrBlank()) {
                    _uiState.value = MealUiState.ExternalEndpointMissing
                    return@launch
                }
            } else {
                if (apiKey.isNullOrBlank()) {
                    _uiState.value = MealUiState.ApiKeyMissing
                    return@launch
                }
                cachedApiKey = apiKey
            }

            _uiState.value = MealUiState.Loading

            val (dateStr, displayDate) = getTargetDateInfo()

            repository.getMeal(dateStr)
                .onSuccess { meal ->
                    if (meal != null) {
                        _uiState.value = MealUiState.Success(meal, displayDate)
                        updateWidget()
                    } else {
                        val key = cachedApiKey
                        if (!key.isNullOrBlank() && source != AppPreferences.SOURCE_EXTERNAL) {
                            syncAndRetry(key, dateStr, displayDate)
                        } else {
                            _uiState.value = MealUiState.Success(null, displayDate)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.value = MealUiState.Error(
                        "데이터 로드 실패: ${e.localizedMessage}",
                        source == AppPreferences.SOURCE_EXTERNAL
                    )
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
            preferences.clearApiKey()
            preferences.updateSyncStatus(0, 0)
            cachedApiKey = null
            _uiState.value = MealUiState.ApiKeyMissing
        }
    }

    fun resetExternalEndpoint() {
        viewModelScope.launch {
            preferences.clearExternalApiEndpoint()
            _uiState.value = MealUiState.ExternalEndpointMissing
        }
    }

    fun resetForError(isExternalSource: Boolean) {
        if (isExternalSource) {
            resetExternalEndpoint()
        } else {
            resetApiKey()
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

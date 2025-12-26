package com.sleepysoong.armydiet.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u0013\u001a\u00020\u0014H\u0002J\u0006\u0010\u0015\u001a\u00020\u0014J\u0006\u0010\u0016\u001a\u00020\u0014J\u0006\u0010\u0017\u001a\u00020\u0014J\u000e\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0019\u001a\u00020\u000bR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001d\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\u000e0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\t0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0010\u00a8\u0006\u001a"}, d2 = {"Lcom/sleepysoong/armydiet/ui/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/sleepysoong/armydiet/domain/MealRepository;", "preferences", "Lcom/sleepysoong/armydiet/data/local/AppPreferences;", "(Lcom/sleepysoong/armydiet/domain/MealRepository;Lcom/sleepysoong/armydiet/data/local/AppPreferences;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/sleepysoong/armydiet/ui/MealUiState;", "currentApiKey", "", "debugLogs", "Lkotlinx/coroutines/flow/StateFlow;", "", "getDebugLogs", "()Lkotlinx/coroutines/flow/StateFlow;", "uiState", "getUiState", "checkApiKeyAndLoad", "", "clearLogs", "loadMeal", "resetApiKey", "saveApiKey", "key", "app_debug"})
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.sleepysoong.armydiet.domain.MealRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.sleepysoong.armydiet.data.local.AppPreferences preferences = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.sleepysoong.armydiet.ui.MealUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.sleepysoong.armydiet.ui.MealUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> debugLogs = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentApiKey;
    
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    com.sleepysoong.armydiet.domain.MealRepository repository, @org.jetbrains.annotations.NotNull()
    com.sleepysoong.armydiet.data.local.AppPreferences preferences) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.sleepysoong.armydiet.ui.MealUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> getDebugLogs() {
        return null;
    }
    
    private final void checkApiKeyAndLoad() {
    }
    
    public final void saveApiKey(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
    }
    
    public final void loadMeal() {
    }
    
    public final void resetApiKey() {
    }
    
    public final void clearLogs() {
    }
}
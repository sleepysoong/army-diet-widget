package com.sleepysoong.armydiet.domain;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010$\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\nH\u0002J&\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\r2\u0006\u0010\u000f\u001a\u00020\nH\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0010\u0010\u0011J\u0018\u0010\u0012\u001a\u00020\n2\u0006\u0010\u0013\u001a\u00020\n2\u0006\u0010\u0014\u001a\u00020\nH\u0002J\u001a\u0010\u0015\u001a\u00020\n2\u0006\u0010\u0016\u001a\u00020\n2\b\u0010\u0017\u001a\u0004\u0018\u00010\nH\u0002J\u0012\u0010\u0018\u001a\u0004\u0018\u00010\n2\u0006\u0010\u0019\u001a\u00020\nH\u0002J\"\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000e0\u001b2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001dH\u0002J\u0010\u0010\u001f\u001a\u00020\n2\u0006\u0010 \u001a\u00020\nH\u0002J\u0016\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0011J\"\u0010$\u001a\u00020\"2\u0012\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000e0\u001bH\u0082@\u00a2\u0006\u0002\u0010&R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\'"}, d2 = {"Lcom/sleepysoong/armydiet/domain/MealRepository;", "", "mealDao", "Lcom/sleepysoong/armydiet/data/local/MealDao;", "api", "Lcom/sleepysoong/armydiet/data/remote/MndApi;", "(Lcom/sleepysoong/armydiet/data/local/MealDao;Lcom/sleepysoong/armydiet/data/remote/MndApi;)V", "allergyRegex", "Lkotlin/text/Regex;", "cleanText", "", "text", "getMeal", "Lkotlin/Result;", "Lcom/sleepysoong/armydiet/data/local/MealEntity;", "date", "getMeal-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "mergeMenuItems", "oldStr", "newStr", "mergeStrings", "current", "new", "parseDate", "dateStr", "processRows", "", "rows", "", "Lcom/sleepysoong/armydiet/data/remote/MndRow;", "sortAndJoin", "raw", "syncRecentData", "", "apiKey", "upsertMeals", "newMeals", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class MealRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.sleepysoong.armydiet.data.local.MealDao mealDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.sleepysoong.armydiet.data.remote.MndApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.text.Regex allergyRegex = null;
    
    public MealRepository(@org.jetbrains.annotations.NotNull()
    com.sleepysoong.armydiet.data.local.MealDao mealDao, @org.jetbrains.annotations.NotNull()
    com.sleepysoong.armydiet.data.remote.MndApi api) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object syncRecentData(@org.jetbrains.annotations.NotNull()
    java.lang.String apiKey, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.util.Map<java.lang.String, com.sleepysoong.armydiet.data.local.MealEntity> processRows(java.util.List<com.sleepysoong.armydiet.data.remote.MndRow> rows) {
        return null;
    }
    
    private final java.lang.String mergeStrings(java.lang.String current, java.lang.String p1_54480) {
        return null;
    }
    
    private final java.lang.Object upsertMeals(java.util.Map<java.lang.String, com.sleepysoong.armydiet.data.local.MealEntity> newMeals, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.String mergeMenuItems(java.lang.String oldStr, java.lang.String newStr) {
        return null;
    }
    
    private final java.lang.String sortAndJoin(java.lang.String raw) {
        return null;
    }
    
    private final java.lang.String parseDate(java.lang.String dateStr) {
        return null;
    }
    
    private final java.lang.String cleanText(java.lang.String text) {
        return null;
    }
}
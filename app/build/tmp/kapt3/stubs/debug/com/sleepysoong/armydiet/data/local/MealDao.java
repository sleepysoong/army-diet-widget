package com.sleepysoong.armydiet.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\"\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00030\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u000b\u001a\u00020\f2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00030\bH\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u000e"}, d2 = {"Lcom/sleepysoong/armydiet/data/local/MealDao;", "", "getMeal", "Lcom/sleepysoong/armydiet/data/local/MealEntity;", "date", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getMealsByDates", "", "dates", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertMeals", "", "meals", "app_debug"})
@androidx.room.Dao()
public abstract interface MealDao {
    
    @androidx.room.Query(value = "SELECT * FROM meals WHERE date = :date")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMeal(@org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.sleepysoong.armydiet.data.local.MealEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM meals WHERE date IN (:dates)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMealsByDates(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> dates, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.sleepysoong.armydiet.data.local.MealEntity>> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertMeals(@org.jetbrains.annotations.NotNull()
    java.util.List<com.sleepysoong.armydiet.data.local.MealEntity> meals, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}
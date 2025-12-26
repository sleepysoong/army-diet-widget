package com.sleepysoong.armydiet.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J,\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00072\b\b\u0001\u0010\b\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\t\u00a8\u0006\n"}, d2 = {"Lcom/sleepysoong/armydiet/data/remote/MndApi;", "", "getMeals", "Lcom/sleepysoong/armydiet/data/remote/MndResponse;", "apiKey", "", "startIndex", "", "endIndex", "(Ljava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface MndApi {
    
    @retrofit2.http.GET(value = "{apiKey}/json/DS_TB_MNDT_DATEBYMLSVC_7369/{startIndex}/{endIndex}/")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMeals(@retrofit2.http.Path(value = "apiKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String apiKey, @retrofit2.http.Path(value = "startIndex")
    int startIndex, @retrofit2.http.Path(value = "endIndex")
    int endIndex, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.sleepysoong.armydiet.data.remote.MndResponse> $completion);
}
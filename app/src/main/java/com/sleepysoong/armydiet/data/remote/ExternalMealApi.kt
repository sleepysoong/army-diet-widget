package com.sleepysoong.armydiet.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class ExternalResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)

data class ExternalMenu(
    val date: String,
    val breakfast: List<String>?,
    val lunch: List<String>?,
    val dinner: List<String>?,
    val total_calories: String?
)

interface ExternalMealApi {
    @GET("api/menu/today")
    suspend fun getToday(): ExternalResponse<ExternalMenu>

    @GET("api/menu")
    suspend fun getMenu(
        @Query("date") date: String,
        @Query("meal") meal: String? = null
    ): ExternalResponse<ExternalMenu>
}

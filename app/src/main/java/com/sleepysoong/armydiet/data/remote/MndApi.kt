package com.sleepysoong.armydiet.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface MndApi {
    // URL Structure: {apiKey}/json/DS_TB_MNDT_DATEBYMLSVC_7369/{startIndex}/{endIndex}/

    @GET("{apiKey}/json/DS_TB_MNDT_DATEBYMLSVC_7369/{startIndex}/{endIndex}/")
    suspend fun getMeals(
        @Path("apiKey") apiKey: String,
        @Path("startIndex") startIndex: Int,
        @Path("endIndex") endIndex: Int
    ): MndResponse
}
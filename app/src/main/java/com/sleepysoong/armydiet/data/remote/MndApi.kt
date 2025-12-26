package com.sleepysoong.armydiet.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface MndApi {
    // API 문서에 따르면 끝에 슬래시(/)가 있는 샘플이 있습니다.
    // Retrofit에서는 Path 변환 후 URL 인코딩 이슈가 있을 수 있으니 주의.
    // http://openapi.mnd.go.kr/{KEY}/json/DS_TB_MNDT_DATEBYMLSVC_7369/{START}/{END}/
    
    @GET("{apiKey}/json/DS_TB_MNDT_DATEBYMLSVC_7369/{startIndex}/{endIndex}/")
    suspend fun getMeals(
        @Path("apiKey") apiKey: String,
        @Path("startIndex") startIndex: Int,
        @Path("endIndex") endIndex: Int
    ): MndResponse
}

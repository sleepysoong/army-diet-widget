package com.sleepysoong.armydiet.data.remote

import com.sleepysoong.armydiet.util.DebugLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://openapi.mnd.go.kr/"

    // 기존 로깅 인터셉터 + 커스텀 디버그 로거
    private val debugInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        DebugLogger.log("Net", "Request: ${request.method} ${request.url}")
        
        try {
            val response = chain.proceed(request)
            DebugLogger.log("Net", "Response: ${response.code} ${response.message}")
            
            // Body를 읽으면 스트림이 소비되므로, peekBody를 사용하거나 로직이 복잡해짐.
            // 여기서는 간단히 성공/실패 여부와 URL만 남김. 상세 내용은 LoggingInterceptor가 로그캣엔 찍어줌.
            if (!response.isSuccessful) {
                DebugLogger.log("Net", "Fail Body: ${response.peekBody(1024).string()}")
            }
            response
        } catch (e: Exception) {
            DebugLogger.log("Net", "Failure: ${e.message}")
            throw e
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(debugInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: MndApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MndApi::class.java)
}
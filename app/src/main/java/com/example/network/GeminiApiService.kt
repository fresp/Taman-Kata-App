package com.example.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface GeminiApiService {
    companion object {
        // Model: gemini-3.6-flash (dicek 14 Agustus 2026).
        // Google sering deprecate model Gemini cukup cepat — kalau muncul
        // error 404 "no longer available to new users" lagi di masa depan,
        // cek model terbaru di https://ai.google.dev/gemini-api/docs/models
        // dan ganti di sini.
        const val MODEL_NAME = "gemini-3.6-flash"
    }

    @POST("v1beta/models/$MODEL_NAME:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    // Ganti URL ini dengan URL Cloud Run Anda saat deployment
    // Contoh: "https://taman-kata-backend-xxxxx.a.run.app/"
    private const val CUSTOM_BACKEND_URL = "" 
    private val BASE_URL = CUSTOM_BACKEND_URL.ifEmpty { "https://generativelanguage.googleapis.com/" }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

package com.example.data.sync

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CloudSyncApi {
    @POST("api/auth/login")
    suspend fun authenticateUser(
        @Body request: AuthRequest
    ): AuthResponse

    @POST("api/sync/upload")
    suspend fun uploadExpenses(
        @Header("Authorization") authHeader: String,
        @Body payload: SyncPayload
    ): SyncResponse

    @GET("api/sync/download")
    suspend fun downloadExpenses(
        @Header("Authorization") authHeader: String,
        @Query("userId") userId: String
    ): SyncResponse
}

object CloudSyncService {
    private var customBaseUrl: String = "https://example.com/" // Default placeholder URL

    fun updateBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (formatted.startsWith("http://") || formatted.startsWith("https://")) {
            customBaseUrl = formatted
        }
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val api: CloudSyncApi by lazy {
        Retrofit.Builder()
            .baseUrl(customBaseUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CloudSyncApi::class.java)
    }
}

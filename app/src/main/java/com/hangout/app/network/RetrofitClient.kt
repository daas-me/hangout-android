package com.hangout.app.network

import android.content.Context
import com.hangout.app.utils.SessionManager
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "https://hangout-rp28.onrender.com/api/"

    // For emulator use 10.0.2.2, for real device use your PC's local IP
    // e.g. "http://192.168.1.10:8080/api/"
    const val FALLBACK_URL = "http://10.0.2.2:8080/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val fallbackInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        try {
            // Try original request first
            chain.proceed(originalRequest)
        } catch (e: IOException) {
            // If it fails, rebuild the request pointing to localhost
            val fallbackBase = FALLBACK_URL.toHttpUrl()
            val fallbackUrl = originalRequest.url.newBuilder()
                .scheme(fallbackBase.scheme)
                .host(fallbackBase.host)
                .port(fallbackBase.port)
                .build()

            val fallbackRequest = originalRequest.newBuilder()
                .url(fallbackUrl)
                .build()

            chain.proceed(fallbackRequest)
        }
    }

    fun getApiService(context: Context): ApiService {
        val session = SessionManager(context)

        val authInterceptor = Interceptor { chain ->
            val token = session.getToken()
            val request = chain.request().newBuilder().apply {
                if (token != null) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(fallbackInterceptor) // 👈 add before logging
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
package com.hangout.app.network

import com.hangout.app.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    // ── User Profile ──────────────────────────────────────────────────────
    @GET("user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserProfile>

    @PUT("user/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<MessageResponse>

    @PUT("user/password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<MessageResponse>

    @GET("user/stats")
    suspend fun getStats(@Header("Authorization") token: String): Response<UserStats>

    // ── Photo ─────────────────────────────────────────────────────────────
    @Multipart
    @POST("user/photo")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Part photo: MultipartBody.Part
    ): Response<MessageResponse>

    @GET("user/photo")
    suspend fun getPhoto(@Header("Authorization") token: String): Response<PhotoResponse>

    @DELETE("user/photo")
    suspend fun deletePhoto(@Header("Authorization") token: String): Response<MessageResponse>

    @GET("events/hosting")
    suspend fun getHostingEvents(
        @Header("Authorization") token: String
    ): Response<List<EventItem>>

    @GET("events/today")
    suspend fun getTodayEvents(
        @Header("Authorization") token: String
    ): Response<List<EventItem>>

    @GET("events/discover")
    suspend fun getDiscoverEvents(
        @Header("Authorization") token: String,
        @Query("search") search: String = "",
        @Query("filter") filter: String = ""
    ): Response<List<EventItem>>

}


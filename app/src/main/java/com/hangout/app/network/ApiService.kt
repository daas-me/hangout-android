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
    suspend fun getProfile(): Response<UserProfile>

    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<MessageResponse>

    @PUT("user/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<MessageResponse>

    @GET("user/stats")
    suspend fun getStats(): Response<UserStats>

    // ── Photo ─────────────────────────────────────────────────────────────
    @Multipart
    @POST("user/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<MessageResponse>

    @GET("user/photo")
    suspend fun getPhoto(): Response<PhotoResponse>

    @DELETE("user/photo")
    suspend fun deletePhoto(): Response<MessageResponse>

    // ── Events ────────────────────────────────────────────────────────────
    @GET("events/hosting")
    suspend fun getHostingEvents(): Response<List<EventItem>>

    @GET("events/today")
    suspend fun getTodayEvents(): Response<List<EventItem>>

    @GET("events/discover")
    suspend fun getDiscoverEvents(
        @Query("search") search: String = "",
        @Query("filter") filter: String = ""
    ): Response<List<EventItem>>
}
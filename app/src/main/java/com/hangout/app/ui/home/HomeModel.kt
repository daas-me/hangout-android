package com.hangout.app.ui.home

import android.content.Context
import com.hangout.app.models.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository

class HomeModel(context: Context) {

    private val userRepo = UserRepository(context)
    private val api      = RetrofitClient.getApiService(context)

    suspend fun getProfile(): Result<UserProfile> = userRepo.getProfile()

    suspend fun getStats(): Result<UserStats> = userRepo.getStats()

    suspend fun getHostingEvents(): Result<List<EventItem>> {
        return try {
            val response = api.getHostingEvents()
            if (response.isSuccessful)
                Result.Success(response.body() ?: emptyList())
            else
                Result.Error("Failed to load hosting events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getTodayEvents(): Result<List<EventItem>> {
        return try {
            val response = api.getTodayEvents()
            if (response.isSuccessful)
                Result.Success(response.body() ?: emptyList())
            else
                Result.Error("Failed to load today's events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

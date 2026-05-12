package com.hangout.app.ui.home

import android.content.Context
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import com.hangout.app.utils.AppCache


class HomeModel(private val context: Context) {

    private val userRepo = UserRepository(context)
    private val api      = RetrofitClient.getApiService(context)

    suspend fun getProfile(): Result<UserProfile> = userRepo.getProfile()

    suspend fun getStats(): Result<UserStats> = userRepo.getStats()

    suspend fun getHostingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.HOSTING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<List<EventItem>>() {}.type
                return Result.Success(com.google.gson.Gson().fromJson(json, type))
            }
        }
        return try {
            val response = api.getHostingEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load hosting events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getTodayEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.TODAY_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<List<EventItem>>() {}.type
                return Result.Success(com.google.gson.Gson().fromJson(json, type))
            }
        }
        return try {
            val response = api.getTodayEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load today's events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

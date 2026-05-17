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

    suspend fun getStats(): Result<UserStats> {
        return try {
            // Get hosting events (published only)
            val hostingResult = getHostingEvents()
            val hostingCount = when (hostingResult) {
                is Result.Success -> hostingResult.data.size.toLong()
                else -> 0L
            }

            // Get attending events
            val attendingResult = getAttendingEvents()
            val attendingCount = when (attendingResult) {
                is Result.Success -> attendingResult.data.size
                else -> 0
            }

            // Calculate total attendees from published hosting events
            val hostingEventsResult = getHostingEvents()
            val totalAttendees = when (hostingEventsResult) {
                is Result.Success -> hostingEventsResult.data.sumOf { it.attendeeCount ?: 0 }
                else -> 0
            }

            Result.Success(UserStats(
                hostingCount = hostingCount,
                attendingCount = attendingCount,
                totalAttendees = totalAttendees
            ))
        } catch (e: Exception) {
            Result.Error("Failed to calculate stats.")
        }
    }

    suspend fun getHostingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.HOSTING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<List<EventItem>>() {}.type
                val events = com.google.gson.Gson().fromJson<List<EventItem>>(json, type)
                // Filter to show only published events (isDraft == false)
                return Result.Success(events.filter { it.isDraft != true })
            }
        }
        return try {
            val response = api.getHostingEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                // Filter to show only published events (isDraft == false)
                val publishedEvents = body.filter { it.isDraft != true }
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(publishedEvents)
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
                val events = com.google.gson.Gson().fromJson<List<EventItem>>(json, type)
                // Filter to show only published events (isDraft == false)
                return Result.Success(events.filter { it.isDraft != true })
            }
        }
        return try {
            val response = api.getTodayEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                // Filter to show only published events (isDraft == false)
                val publishedEvents = body.filter { it.isDraft != true }
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(publishedEvents)
            } else Result.Error("Failed to load today's events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getAttendingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.ATTENDING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<List<EventItem>>() {}.type
                val events = com.google.gson.Gson().fromJson<List<EventItem>>(json, type)
                // Filter to show only confirmed attending events
                return Result.Success(events.filter { it.status == "confirmed" })
            }
        }
        return try {
            val response = api.getAttendingEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                // Filter to show only confirmed attending events
                val confirmedEvents = body.filter { it.status == "confirmed" }
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(confirmedEvents)
            } else Result.Error("Failed to load attending events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

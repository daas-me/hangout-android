package com.hangout.app.ui.myhangouts

import android.content.Context
import com.hangout.app.data.EventItem
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class MyHangoutsModel(private val context: Context) {

    private val eventRepo = EventRepository(context)
    private val api       = RetrofitClient.getApiService(context)

    suspend fun getAttendingEvents(): Result<List<EventItem>> =
        eventRepo.getAttendingEvents()

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

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> =
        eventRepo.cancelRsvp(eventId)
}
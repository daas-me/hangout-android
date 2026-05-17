package com.hangout.app.ui.myhangouts

import android.content.Context
import com.hangout.app.data.EventItem
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

class MyHangoutsModel(context: Context) {

    private val eventRepo = EventRepository(context)
    private val api       = RetrofitClient.getApiService(context)

    suspend fun getAttendingEvents(): Result<List<EventItem>> = eventRepo.getAttendingEvents()

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

    suspend fun getFavoriteEvents(): Result<List<EventItem>> = eventRepo.getFavoriteEvents()

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> = eventRepo.cancelRsvp(eventId)

    suspend fun unfavorite(eventId: Long): Result<MessageResponse> = eventRepo.removeFavorite(eventId)
}
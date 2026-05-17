package com.hangout.app.ui.createevent

import android.content.Context
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.data.CreateEventResponse
import com.hangout.app.data.EventItem
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.CreateEventRepository
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache
import java.io.File

class CreateEventModel(private val context: Context) {

    private val repo = CreateEventRepository(context)
    private val api  = RetrofitClient.getApiService(context)

    suspend fun createEvent(request: CreateEventRequest): Result<CreateEventResponse> =
        repo.createEvent(request)

    suspend fun uploadCoverImage(eventId: Long, imageFile: File): Result<String> =
        repo.uploadCoverImage(eventId, imageFile)

    suspend fun loadEventForEdit(eventId: Long): Result<EventItem> = try {
        val response = api.getEventById(eventId)
        if (response.isSuccessful) {
            val event = response.body()
            if (event != null) Result.Success(event)
            else Result.Error("Event data not found")
        } else {
            Result.Error("Failed to load event (${response.code()})")
        }
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    suspend fun unpublishEvent(eventId: Long, reason: String): Result<MessageResponse> = try {
        val response = api.cancelEvent(eventId, mapOf("reason" to reason))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
            AppCache.bust(context, AppCache.Keys.STATS)
            Result.Success(response.body() ?: MessageResponse("Event unpublished"))
        } else {
            Result.Error("Failed to unpublish event (${response.code()})")
        }
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }
}
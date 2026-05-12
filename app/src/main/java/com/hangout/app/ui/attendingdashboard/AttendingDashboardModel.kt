package com.hangout.app.ui.attendingdashboard

import android.content.Context
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

class AttendingDashboardModel(context: Context) {

    private val eventRepo = EventRepository(context)
    private val api       = RetrofitClient.getApiService(context)

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> =
        eventRepo.cancelRsvp(eventId)

    suspend fun requestRefund(eventId: Long, reason: String): Result<MessageResponse> {
        return try {
            val response = api.requestRefund(eventId, mapOf("reason" to reason))
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Refund requested"))
            else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun acknowledgeRefund(eventId: Long): Result<MessageResponse> {
        return try {
            val response = api.acknowledgeRefund(eventId)
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Acknowledged"))
            else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    private fun parseError(body: String?): String {
        if (body == null) return "Server error"
        return try {
            org.json.JSONObject(body).optString("message", "Server error")
        } catch (_: Exception) { "Server error" }
    }
}
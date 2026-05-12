package com.hangout.app.ui.hostdashboard

import android.content.Context
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class HostDashboardModel(private val context: Context) {

    private val api = RetrofitClient.getApiService(context)


    suspend fun getAttendees(eventId: Long): Result<List<AttendeeItem>> {
        val key = AppCache.Keys.eventAttendees(eventId)
        AppCache.get(context, key)?.let { json ->
            val type = object : com.google.gson.reflect.TypeToken<List<AttendeeItem>>() {}.type
            return Result.Success(com.google.gson.Gson().fromJson(json, type))
        }
        return try {
            val response = api.getEventAttendees(eventId)
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load attendees (${response.code()})")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun approvePayment(
        eventId: Long, rsvpId: Long, seatNumber: String?
    ): Result<MessageResponse> {
        return try {
            val body = ApprovePaymentRequest(seatNumber = seatNumber)
            val response = api.approvePayment(eventId, rsvpId, body)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                Result.Success(response.body() ?: MessageResponse("Approved"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun rejectPayment(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> {
        return try {
            val response = api.rejectPayment(eventId, rsvpId, RejectPaymentRequest(reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Rejected"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun assignSeat(
        eventId: Long, rsvpId: Long, seatNumber: String
    ): Result<MessageResponse> {
        return try {
            val response = api.assignSeat(eventId, rsvpId, AssignSeatRequest(seatNumber))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Seat assigned"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun confirmAttendee(eventId: Long, rsvpId: Long): Result<MessageResponse> {
        return try {
            val response = api.confirmAttendee(eventId, rsvpId)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                Result.Success(response.body() ?: MessageResponse("Confirmed"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun rejectAttendee(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> {
        return try {
            val response = api.rejectAttendee(eventId, rsvpId, RejectPaymentRequest(reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                Result.Success(response.body() ?: MessageResponse("Rejected"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun cancelEvent(eventId: Long, reason: String): Result<MessageResponse> {
        return try {
            val response = api.cancelEvent(eventId, mapOf("reason" to reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Event cancelled"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deleteEvent(eventId: Long): Result<MessageResponse> {
        return try {
            val response = api.deleteEvent(eventId)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Event deleted"))
            } else
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
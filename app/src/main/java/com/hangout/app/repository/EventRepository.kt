package com.hangout.app.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.AppCache

class EventRepository(private val context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    // ── Attending events ──────────────────────────────────────────────────

    suspend fun getAttendingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.ATTENDING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val r = api.getAttendingEvents()
            if (r.isSuccessful) {
                val body = r.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load attending events")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    // ── RSVP — bust attending cache after any RSVP action ─────────────────

    suspend fun rsvpEvent(eventId: Long): Result<RsvpResponse> {
        return try {
            val r = api.rsvpEvent(eventId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(r.body() ?: RsvpResponse(null, null, null, true, "RSVP'd"))
            } else {
                val msg = try {
                    org.json.JSONObject(r.errorBody()?.string() ?: "")
                        .optString("message", "RSVP failed")
                } catch (_: Exception) { "RSVP failed" }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> {
        return try {
            val r = api.cancelRsvp(eventId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                Result.Success(r.body() ?: MessageResponse("Cancelled"))
            } else Result.Error("Cancel failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun checkRsvpStatus(eventId: Long): Result<RsvpStatusResponse> {
        return try {
            val r = api.checkRsvpStatus(eventId)
            if (r.isSuccessful)
                Result.Success(r.body() ?: RsvpStatusResponse(false, null, null))
            else Result.Error("Status check failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
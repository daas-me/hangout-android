package com.hangout.app.ui.hostdashboard

import android.content.Context
import android.net.Uri
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class HostDashboardModel(private val context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    // ── Attendees ──────────────────────────────────────────────

    suspend fun getAttendees(eventId: Long, forceRefresh: Boolean = false): Result<List<AttendeeItem>> {
        val key = AppCache.Keys.eventAttendees(eventId)
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<AttendeeItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val response = api.getEventAttendees(eventId)
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load attendees (${response.code()})")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    // ── Payment ────────────────────────────────────────────────

    suspend fun approvePayment(
        eventId: Long, rsvpId: Long, seatNumber: String?
    ): Result<MessageResponse> = try {
        val response = api.approvePayment(eventId, rsvpId, ApprovePaymentRequest(seatNumber))
        if (response.isSuccessful) {
            bustAttendeeAndStats(eventId)
            Result.Success(response.body() ?: MessageResponse("Approved"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    suspend fun rejectPayment(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> = try {
        val response = api.rejectPayment(eventId, rsvpId, RejectPaymentRequest(reason))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Rejected"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    // ── Seating ────────────────────────────────────────────────

    suspend fun assignSeat(
        eventId: Long, rsvpId: Long, seatNumber: String
    ): Result<MessageResponse> = try {
        val response = api.assignSeat(eventId, rsvpId, AssignSeatRequest(seatNumber))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Seat assigned"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    // ── Attendee status ────────────────────────────────────────

    suspend fun confirmAttendee(eventId: Long, rsvpId: Long): Result<MessageResponse> = try {
        val response = api.confirmAttendee(eventId, rsvpId)
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            Result.Success(response.body() ?: MessageResponse("Confirmed"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    suspend fun rejectAttendee(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> = try {
        val response = api.rejectAttendee(eventId, rsvpId, RejectPaymentRequest(reason))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            Result.Success(response.body() ?: MessageResponse("Rejected"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    // ── Refund (host side) ─────────────────────────────────────

    /**
     * Host marks a refund as processed.
     * Uploads an optional proof image alongside an optional reference note.
     * Moves attendee refundStatus → "waiting_acknowledgement".
     *
     * The request is sent as multipart/form-data:
     *   - Part "note"       : plain-text reference note (may be empty)
     *   - Part "proofImage" : JPEG/PNG image bytes (optional)
     */
    suspend fun approveRefund(
        eventId: Long,
        rsvpId: Long,
        note: String,
        proofUri: Uri?
    ): Result<MessageResponse> = try {

        val imagePart: MultipartBody.Part? = proofUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.Error("Could not open the selected image.")
            val rawBytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return Result.Error("Could not decode the selected image. Try a different file.")

            val out = java.io.ByteArrayOutputStream()
            var quality = 90
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            while (out.toByteArray().size > 4 * 1024 * 1024 && quality > 20) {
                out.reset()
                quality -= 20
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            val bytes = out.toByteArray()
            android.util.Log.d("approveRefund", "Image compressed to ${bytes.size} bytes at quality $quality")

            MultipartBody.Part.createFormData(
                "refundProof",
                "refund_proof_${System.currentTimeMillis()}.jpg",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
        }

        val response = api.approveRefund(eventId, rsvpId, imagePart!!)
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Refund processed"))
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.e("approveRefund", "Server error ${response.code()}: $errorBody")
            Result.Error(parseError(errorBody))
        }

    } catch (e: Exception) {
        android.util.Log.e("approveRefund", "Exception in approveRefund", e)
        Result.Error("Error: ${e.message}")
    }

    /**
     * Host declines a refund request.
     * Moves refundStatus → "rejected".
     */
    suspend fun rejectRefund(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> = try {
        val response = api.rejectRefund(eventId, rsvpId, mapOf("reason" to reason))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Refund rejected"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    // ── Event lifecycle ────────────────────────────────────────

    suspend fun cancelEvent(eventId: Long, reason: String): Result<MessageResponse> = try {
        val response = api.cancelEvent(eventId, mapOf("reason" to reason))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
            AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
            AppCache.bust(context, AppCache.Keys.STATS)
            AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
            AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Event cancelled"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    suspend fun deleteEvent(eventId: Long): Result<MessageResponse> = try {
        val response = api.deleteEvent(eventId)
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
            AppCache.bust(context, AppCache.Keys.STATS)
            AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
            Result.Success(response.body() ?: MessageResponse("Event deleted"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    suspend fun markAttendance(
        eventId: Long, rsvpId: Long, status: String
    ): Result<MessageResponse> = try {
        val response = api.markAttendance(eventId, rsvpId, mapOf("status" to status))
        if (response.isSuccessful) {
            AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
            Result.Success(response.body() ?: MessageResponse("Attendance updated"))
        } else Result.Error(parseError(response.errorBody()?.string()))
    } catch (e: Exception) {
        Result.Error("Cannot connect to server.")
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun bustAttendeeAndStats(eventId: Long) {
        AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
        AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
        AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
        AppCache.bust(context, AppCache.Keys.STATS)
    }

    private fun parseError(body: String?): String {
        if (body == null) return "Server error"
        return try {
            org.json.JSONObject(body).optString("message", "Server error")
        } catch (_: Exception) { "Server error" }
    }
}
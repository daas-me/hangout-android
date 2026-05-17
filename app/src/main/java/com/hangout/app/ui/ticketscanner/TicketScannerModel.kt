package com.hangout.app.ui.ticketscanner

import android.content.Context
import com.google.gson.Gson
import com.hangout.app.data.TicketVerifyResult
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result

class TicketScannerModel(context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    suspend fun verifyTicket(eventId: Long, ticketToken: String): Result<TicketVerifyResult> {
        return try {
            val response = api.verifyTicket(eventId, ticketToken)
            if (response.isSuccessful && response.body() != null) {
                // Backend returns Map<String, Any>, convert via Gson for type safety
                val json   = gson.toJson(response.body())
                val result = gson.fromJson(json, TicketVerifyResult::class.java)
                Result.Success(result)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = try {
                    org.json.JSONObject(errorBody ?: "")
                        .optString("message", "Verification failed")
                } catch (_: Exception) { "Verification failed (${response.code()})" }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun markAttendance(eventId: Long, rsvpId: Long, status: String): Result<com.hangout.app.data.MessageResponse> {
        return try {
            val body = mapOf("status" to status)  // Match HostDashboardModel API expectation
            val response = api.markAttendance(eventId, rsvpId, body)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: com.hangout.app.data.MessageResponse("Attendance marked"))
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = try {
                    org.json.JSONObject(errorBody ?: "")
                        .optString("message", "Failed to mark attendance")
                } catch (_: Exception) { "Failed to mark attendance (${response.code()})" }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
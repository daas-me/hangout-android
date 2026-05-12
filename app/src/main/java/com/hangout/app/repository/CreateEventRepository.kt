package com.hangout.app.repository

import android.content.Context
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.data.CreateEventResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class CreateEventRepository(context: Context) {

    private val api     = RetrofitClient.getApiService(context)
    private val session = SessionManager(context)

    /** Create or update an event (isDraft flag controls draft vs publish). */
    suspend fun createEvent(request: CreateEventRequest): Result<CreateEventResponse> {
        return try {
            val response = api.createEvent(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.Success(body)
                else Result.Error("Empty response from server")
            } else {
                val errorMsg = response.errorBody()?.string()
                    ?.let { parseErrorMessage(it) }
                    ?: "Failed to create event (${response.code()})"
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server. Check your connection.")
        }
    }

    /** Upload a cover image for an event; returns the image URL. */
    suspend fun uploadCoverImage(eventId: Long, imageFile: File): Result<String> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
            val response = api.uploadEventCoverImage(eventId, part)
            if (response.isSuccessful) {
                val url = response.body()?.get("imageUrl") as? String
                if (url != null) Result.Success(url)
                else Result.Error("No image URL in response")
            } else {
                Result.Error("Image upload failed (${response.code()})")
            }
        } catch (e: Exception) {
            Result.Error("Image upload error: ${e.message}")
        }
    }

    private fun parseErrorMessage(json: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("message", "Server error")
        } catch (_: Exception) {
            "Server error"
        }
    }
}
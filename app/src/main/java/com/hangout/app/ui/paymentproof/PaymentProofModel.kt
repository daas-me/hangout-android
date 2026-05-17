package com.hangout.app.ui.paymentproof

import android.content.Context
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class PaymentProofModel(private val context: Context) {
    private val api = RetrofitClient.getApiService(context)

    suspend fun uploadPaymentProof(eventId: Long, imageFile: File): Result<MessageResponse> {
        return try {
            val mimeType = when (imageFile.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png"         -> "image/png"
                "webp"        -> "image/webp"
                else          -> "image/jpeg"
            }
            val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("paymentProof", imageFile.name, requestFile)
            val response = api.uploadPaymentProof(eventId, part)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Submitted"))
                Result.Success(response.body() ?: MessageResponse("Submitted"))
            } else {
                // Parse error body in its own try-catch — Connection: close can cause
                // errorBody().string() to throw, which was falling into the outer catch
                // and showing "Cannot connect to server" even though the request succeeded.
                val msg = try {
                    val raw = response.errorBody()?.string().orEmpty()
                    if (raw.isNotBlank()) {
                        org.json.JSONObject(raw).optString("message", "Submission failed (${response.code()})")
                    } else {
                        "Submission failed (${response.code()})"
                    }
                } catch (_: Exception) {
                    "Submission failed (${response.code()})"
                }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.localizedMessage ?: "Could not reach server."}")
        }
    }
}
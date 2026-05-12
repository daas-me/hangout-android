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
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("proof", imageFile.name, requestFile)
            val response = api.uploadPaymentProof(eventId, part)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Submitted"))
            } else {
                val msg = try {
                    org.json.JSONObject(response.errorBody()?.string() ?: "")
                        .optString("message", "Submission failed")
                } catch (_: Exception) { "Submission failed" }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
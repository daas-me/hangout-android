package com.hangout.app.repository

import com.hangout.app.models.*
import com.hangout.app.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

// ── Auth Repository ───────────────────────────────────────────────────────────

class AuthRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val error = response.errorBody()?.string()
                Result.Error(parseError(error) ?: "Login failed")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server. Check your connection.")
        }
    }

    suspend fun register(
        firstname: String, lastname: String,
        email: String, password: String, birthdate: String
    ): Result<MessageResponse> {
        return try {
            val response = api.register(
                RegisterRequest(firstname, lastname, email, password, birthdate)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val error = response.errorBody()?.string()
                Result.Error(parseError(error) ?: "Registration failed")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server. Check your connection.")
        }
    }
}

// ── User Repository ───────────────────────────────────────────────────────────

class UserRepository {
    private val api = RetrofitClient.apiService

    suspend fun getProfile(token: String): Result<UserProfile> {
        return try {
            val response = api.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null)
                Result.Success(response.body()!!)
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to load profile")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun updateProfile(token: String, firstname: String, lastname: String): Result<MessageResponse> {
        return try {
            val response = api.updateProfile("Bearer $token", UpdateProfileRequest(firstname, lastname))
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Profile updated"))
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to update profile")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun updatePassword(token: String, oldPassword: String, newPassword: String): Result<MessageResponse> {
        return try {
            val response = api.updatePassword("Bearer $token", UpdatePasswordRequest(oldPassword, newPassword))
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Password updated"))
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to update password")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getStats(token: String): Result<UserStats> {
        return try {
            val response = api.getStats("Bearer $token")
            if (response.isSuccessful && response.body() != null)
                Result.Success(response.body()!!)
            else
                Result.Error("Failed to load stats")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun uploadPhoto(token: String, file: File): Result<MessageResponse> {
        return try {
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)
            val response = api.uploadPhoto("Bearer $token", part)
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Photo uploaded"))
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Upload failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getPhoto(token: String): Result<String> {
        return try {
            val response = api.getPhoto("Bearer $token")
            if (response.isSuccessful && response.body() != null)
                Result.Success(response.body()!!.photo)
            else
                Result.Error("No photo")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deletePhoto(token: String): Result<MessageResponse> {
        return try {
            val response = api.deletePhoto("Bearer $token")
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Photo removed"))
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to remove photo")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────

private fun parseError(errorJson: String?): String? {
    if (errorJson.isNullOrBlank()) return null
    return try {
        val obj = org.json.JSONObject(errorJson)
        obj.optString("message").ifBlank { null }
    } catch (e: Exception) { null }
}
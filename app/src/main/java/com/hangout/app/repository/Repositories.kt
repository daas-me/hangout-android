package com.hangout.app.repository

import android.content.Context
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.AppCache
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

// ── Auth Repository ───────────────────────────────────────────────────────────

class AuthRepository(context: Context) {
    private val api = RetrofitClient.getApiService(context)

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
class UserRepository(private val context: Context) {
    private val api  = RetrofitClient.getApiService(context)
    private val gson = com.google.gson.Gson()

    fun clearCache() {
        AppCache.bust(context, AppCache.Keys.PROFILE)
        AppCache.bust(context, AppCache.Keys.STATS)
        AppCache.bust(context, AppCache.Keys.PHOTO)
    }

    suspend fun getProfile(forceRefresh: Boolean = false): Result<UserProfile> {
        if (!forceRefresh) {
            AppCache.get(context, AppCache.Keys.PROFILE)?.let { json ->
                return Result.Success(gson.fromJson(json, UserProfile::class.java))
            }
        }
        return try {
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                AppCache.put(context, AppCache.Keys.PROFILE, gson.toJson(body), AppCache.TTL.PROFILE)
                Result.Success(body)
            } else {
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to load profile")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun updateProfile(firstname: String, lastname: String): Result<MessageResponse> {
        return try {
            val response = api.updateProfile(UpdateProfileRequest(firstname, lastname))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.PROFILE)
                AppCache.bust(context, AppCache.Keys.STATS)
                Result.Success(response.body() ?: MessageResponse("Profile updated"))
            } else {
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to update profile")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<MessageResponse> {
        return try {
            val response = api.updatePassword(UpdatePasswordRequest(oldPassword, newPassword))
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Password updated"))
            else
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to update password")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getStats(forceRefresh: Boolean = false): Result<UserStats> {
        if (!forceRefresh) {
            AppCache.get(context, AppCache.Keys.STATS)?.let { json ->
                return Result.Success(gson.fromJson(json, UserStats::class.java))
            }
        }
        return try {
            val response = api.getStats()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                AppCache.put(context, AppCache.Keys.STATS, gson.toJson(body), AppCache.TTL.PROFILE)
                Result.Success(body)
            } else {
                Result.Error("Failed to load stats")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getPhoto(forceRefresh: Boolean = false): Result<String> {
        if (!forceRefresh) {
            AppCache.get(context, AppCache.Keys.PHOTO)?.let { json ->
                return Result.Success(json)
            }
        }
        return try {
            val response = api.getPhoto()
            if (response.isSuccessful && response.body() != null) {
                val photo = response.body()!!.photo ?: return Result.Error("No photo")
                AppCache.put(context, AppCache.Keys.PHOTO, photo, AppCache.TTL.PROFILE)
                Result.Success(photo)
            } else {
                Result.Error("No photo")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun uploadPhoto(file: File): Result<MessageResponse> {
        return try {
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)
            val response = api.uploadPhoto(part)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.PHOTO)
                Result.Success(response.body() ?: MessageResponse("Photo uploaded"))
            } else {
                Result.Error(parseError(response.errorBody()?.string()) ?: "Upload failed")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deletePhoto(): Result<MessageResponse> {
        return try {
            val response = api.deletePhoto()
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.PHOTO)
                Result.Success(response.body() ?: MessageResponse("Photo removed"))
            } else {
                Result.Error(parseError(response.errorBody()?.string()) ?: "Failed to remove photo")
            }
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
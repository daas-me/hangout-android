package com.hangout.app.repository

import android.content.Context
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

class UserRepository(context: Context) {
    private val api = RetrofitClient.getApiService(context)

    // ── Cache ──────────────────────────────────────────────────────────────
    private var cachedProfile: UserProfile? = null
    private var cachedStats: UserStats? = null
    private var cachedPhoto: String? = null
    private var lastFetchTime: Long = 0L

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private fun isCacheValid() =
        System.currentTimeMillis() - lastFetchTime < CACHE_TTL_MS

    fun clearCache() {
        cachedProfile = null
        cachedStats   = null
        cachedPhoto   = null
        lastFetchTime = 0L
    }

    // ── Profile ────────────────────────────────────────────────────────────

    suspend fun getProfile(forceRefresh: Boolean = false): Result<UserProfile> {
        if (!forceRefresh && isCacheValid() && cachedProfile != null) {
            return Result.Success(cachedProfile!!)
        }
        return try {
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                cachedProfile = response.body()!!
                lastFetchTime = System.currentTimeMillis()
                Result.Success(cachedProfile!!)
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
                clearCache() // bust full cache so next loadAll re-fetches
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

    // ── Stats ──────────────────────────────────────────────────────────────

    suspend fun getStats(forceRefresh: Boolean = false): Result<UserStats> {
        if (!forceRefresh && isCacheValid() && cachedStats != null) {
            return Result.Success(cachedStats!!)
        }
        return try {
            val response = api.getStats()
            if (response.isSuccessful && response.body() != null) {
                cachedStats = response.body()!!
                Result.Success(cachedStats!!)
            } else {
                Result.Error("Failed to load stats")
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    // ── Photo ──────────────────────────────────────────────────────────────

    suspend fun getPhoto(forceRefresh: Boolean = false): Result<String> {
        if (!forceRefresh && isCacheValid() && cachedPhoto != null) {
            return Result.Success(cachedPhoto!!)
        }
        return try {
            val response = api.getPhoto()
            if (response.isSuccessful && response.body() != null) {
                cachedPhoto = response.body()!!.photo
                Result.Success(cachedPhoto!!)
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
                cachedPhoto = null // bust only photo cache
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
                cachedPhoto = null // bust only photo cache
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
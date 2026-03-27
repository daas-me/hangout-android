package com.hangout.app.models

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val email: String,
    val firstname: String
)

data class RegisterRequest(
    val firstname: String,
    val lastname: String,
    val email: String,
    val password: String,
    val birthdate: String       // YYYY-MM-DD
)

// ── User ──────────────────────────────────────────────────────────────────────

data class UserProfile(
    val id: Long,
    val firstname: String,
    val lastname: String,
    val email: String,
    val age: Any?,              // Int or empty string from backend
    val birthdate: String?,
    val role: String
)

data class UpdateProfileRequest(
    val firstname: String,
    val lastname: String
)

data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class UserStats(
    val hostingCount: Long,
    val attendingCount: Int,
    val totalAttendees: Int
)

data class PhotoResponse(
    val photo: String           // Base64 data URI
)

// ── Generic ───────────────────────────────────────────────────────────────────

data class MessageResponse(
    val message: String
)

// ── Session (stored in SharedPreferences) ────────────────────────────────────

data class Session(
    val token: String,
    val email: String,
    val firstname: String
)


data class EventItem(
    val id: Long,
    val title: String,
    val date: String?,
    val time: String?,
    val location: String?,
    val format: String?,
    val price: Double?,
    val capacity: Int?,
    val attendeeCount: Int?,
    val imageUrl: String?,
    val eventType: String?,
    val isDraft: Boolean?
)
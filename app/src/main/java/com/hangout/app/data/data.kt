package com.hangout.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

// ── Events ────────────────────────────────────────────────────────────────────

@Parcelize
data class EventItem(
    val id: Long? = null,
    val title: String = "",
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val format: String? = null,        // "In-Person" | "Virtual" | "Hybrid"
    val price: Double? = null,
    val capacity: Int? = null,
    val attendeeCount: Int? = null,
    val seatingType: String? = null,   // "reserved" | "open"
    val imageUrl: String? = null,
    val paymentMethod: String? = null, // "gcash" | "paymaya" | "bank"
    val accountNumber: String? = null,
    val hostFirstName: String? = null,
    val hostLastName: String? = null,
    val hostEmail: String? = null,
    val eventType: String? = null,
    val isDraft: Boolean? = null
) : Parcelable
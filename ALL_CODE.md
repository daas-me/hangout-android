# HangOut Android App - Complete Code Reference

---

## TABLE OF CONTENTS
1. [Utilities](#utilities)
2. [Data Models](#data-models)
3. [Repositories](#repositories)
4. [Authentication](#authentication)
5. [Navigation](#navigation)
6. [Home Screen](#home-screen)
7. [Discover Screen](#discover-screen)
8. [My HangOuts Screen](#my-hangouts-screen)
9. [Event Detail Screen](#event-detail-screen)
10. [Profile Screen](#profile-screen)
11. [Create Event Screen](#create-event-screen)
12. [Host Dashboard Screen](#host-dashboard-screen)
13. [Payment Proof Screen](#payment-proof-screen)
14. [Custom Components](#custom-components)
15. [Resources](#resources)
16. [Layouts](#layouts)

---

## UTILITIES

### SessionManager.kt
**Path:** `app/src/main/java/com/hangout/app/utils/SessionManager.kt`

```kotlin
package com.hangout.app.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hangout_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN     = "hangout_token"
        private const val KEY_EMAIL     = "hangout_email"
        private const val KEY_FIRSTNAME = "hangout_firstname"
    }

    fun saveSession(token: String, email: String, firstname: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FIRSTNAME, firstname)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getFirstname(): String? = prefs.getString(KEY_FIRSTNAME, null)

    fun getBearerToken(): String = "Bearer ${getToken() ?: ""}"

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
```

### KotlinExt.kt
**Path:** `app/src/main/java/com/hangout/app/utils/KotlinExt.kt`

```kotlin
package com.hangout.app.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

// ── Toast ──────────────────────────────────────────────────────────────────────

fun Activity.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Fragment.toast(message: String) =
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

// ── EditText ───────────────────────────────────────────────────────────────────

fun EditText.getValue(): String = text.toString().trim()

fun EditText.isEmpty(): Boolean = text.toString().isBlank()

// ── View Visibility ────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }

fun View.hide() { visibility = View.GONE }

fun View.invisible() { visibility = View.INVISIBLE }

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

// ── Color ──────────────────────────────────────────────────────────────────────

fun Context.color(res: Int) = ContextCompat.getColor(this, res)

fun Fragment.color(res: Int) = ContextCompat.getColor(requireContext(), res)
```

### EventHolder.kt
**Path:** `app/src/main/java/com/hangout/app/utils/EventHolder.kt`

```kotlin
package com.hangout.app.utils

import com.hangout.app.data.EventItem

object EventHolder {
    var currentEvent: EventItem? = null
}
```

### AppCache.kt
**Path:** `app/src/main/java/com/hangout/app/utils/AppCache.kt`

```kotlin
package com.hangout.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple TTL-based JSON cache backed by SharedPreferences.
 *
 * Usage:
 *   AppCache.get(ctx, "profile")           // returns cached JSON or null
 *   AppCache.put(ctx, "profile", json, 10) // store for 10 minutes
 *   AppCache.bust(ctx, "profile")          // invalidate one key
 *   AppCache.bustAll(ctx)                  // invalidate everything
 */
object AppCache {

    private const val PREFS_NAME = "hangout_cache"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Read ──────────────────────────────────────────────────────────────

    fun get(ctx: Context, key: String): String? {
        val p       = prefs(ctx)
        val expiry  = p.getLong(expiryKey(key), 0L)
        if (System.currentTimeMillis() > expiry) return null          // expired
        return p.getString(dataKey(key), null)
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * @param ttlMinutes how long to keep this entry (default 5 min)
     */
    fun put(ctx: Context, key: String, json: String, ttlMinutes: Long = 5) {
        val expiry = System.currentTimeMillis() + ttlMinutes * 60_000L
        prefs(ctx).edit()
            .putString(dataKey(key),  json)
            .putLong(expiryKey(key),  expiry)
            .apply()
    }

    // ── Invalidation ──────────────────────────────────────────────────────

    fun bust(ctx: Context, key: String) {
        prefs(ctx).edit()
            .remove(dataKey(key))
            .remove(expiryKey(key))
            .apply()
    }

    fun bustPrefix(ctx: Context, prefix: String) {
        val p    = prefs(ctx)
        val edit = p.edit()
        p.all.keys
            .filter { it.startsWith("data_$prefix") }
            .forEach { dataK ->
                val rawKey = dataK.removePrefix("data_")
                edit.remove(dataKey(rawKey))
                edit.remove(expiryKey(rawKey))
            }
        edit.apply()
    }

    fun bustAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    // ── Key helpers ───────────────────────────────────────────────────────

    private fun dataKey(key: String)   = "data_$key"
    private fun expiryKey(key: String) = "exp_$key"

    // ── Predefined keys (avoids typos across the app) ─────────────────────

    object Keys {
        const val PROFILE          = "user_profile"
        const val STATS            = "user_stats"
        const val PHOTO            = "user_photo"
        const val HOSTING_EVENTS   = "hosting_events"
        const val ATTENDING_EVENTS = "attending_events"
        const val TODAY_EVENTS     = "today_events"
        const val DISCOVER_EVENTS  = "discover_events"
        const val NOTIFICATIONS    = "notifications"
        const val UNREAD_NOTIF     = "unread_notifications"
        const val UNREAD_MESSAGES  = "unread_messages"

        // Per-event detail: use "event_detail_<id>"
        fun eventDetail(id: Long) = "event_detail_$id"
        // Per-event attendees: use "event_attendees_<id>"
        fun eventAttendees(id: Long) = "event_attendees_$id"
        // Per-discover search: use "discover_<query>_<filter>"
        fun discoverSearch(search: String, filter: String) =
            "discover_${search}_${filter}".replace(" ", "_")
    }

    // ── TTL constants (minutes) ───────────────────────────────────────────

    object TTL {
        const val PROFILE         = 10L
        const val EVENTS_LIST     = 3L
        const val EVENT_DETAIL    = 5L
        const val NOTIFICATIONS   = 2L
        const val UNREAD          = 1L
    }
}
```

---

## DATA MODELS

### data.kt
**Path:** `app/src/main/java/com/hangout/app/data/data.kt`

```kotlin
package com.hangout.app.data

import android.os.Parcelable

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
```

### EventItem.kt
**Path:** `app/src/main/java/com/hangout/app/data/EventItem.kt`

```kotlin
package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class RsvpResponse(
    val id: Long?,
    val status: String?,
    @SerializedName("paymentStatus") val paymentStatus: String?,
    val rsvped: Boolean = true,
    val message: String?
)

data class RsvpStatusResponse(
    val rsvped: Boolean,
    @SerializedName("paymentStatus") val paymentStatus: String?,
    val status: String?
)

data class EventItem(
    val id: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    @SerializedName("startTime")       val startTime: String? = null,
    @SerializedName("endTime")         val endTime: String? = null,
    val location: String? = null,
    val format: String? = null,
    @SerializedName("eventType")       val eventType: String? = null,
    val price: Double? = null,
    val capacity: Int? = null,
    @SerializedName("attendeeCount")   val attendeeCount: Int? = null,
    @SerializedName("seatingType")     val seatingType: String? = null,
    @SerializedName("imageUrl")        val imageUrl: String? = null,
    @SerializedName("paymentMethod")   val paymentMethod: String? = null,
    @SerializedName("accountName")     val accountName: String? = null,
    @SerializedName("accountNumber")   val accountNumber: String? = null,
    @SerializedName("virtualPlatform") val virtualPlatform: String? = null,
    @SerializedName("virtualLink")     val virtualLink: String? = null,
    @SerializedName("noRefundPolicy")  val noRefundPolicy: Boolean? = null,
    @SerializedName("isDraft")         val isDraft: Boolean? = null,
    @SerializedName("eventStatus")     val eventStatus: String? = null,
    @SerializedName("hostId")          val hostId: Long? = null,
    @SerializedName("hostFirstName")   val hostFirstName: String? = null,
    @SerializedName("hostLastName")    val hostLastName: String? = null,
    @SerializedName("hostEmail")       val hostEmail: String? = null,
    @SerializedName("hostPhoto")       val hostPhoto: String? = null,
    val status: String? = null,
    @SerializedName("paymentStatus")   val rsvpPaymentStatus: String? = null,
    val seatNumber: String? = null,
    val ticketNumber: String? = null,
    @SerializedName("refundStatus")    val refundStatus: String? = null,
    @SerializedName("attendeeStatus")  val attendeeStatus: String? = null
)

### AttendeeModels.kt
**Path:** `app/src/main/java/com/hangout/app/data/AttendeeModels.kt`

```kotlin
package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class AttendeeItem(
    val id: Long,                          // RSVP id
    @SerializedName("userId")      val userId: Long,
    @SerializedName("firstName")   val firstName: String?,
    @SerializedName("lastName")    val lastName: String?,
    @SerializedName("email")       val email: String?,
    @SerializedName("photo")       val photo: String?,
    val status: String?,                   // registered | confirmed | cancelled
    @SerializedName("paymentStatus")   val paymentStatus: String?,   // pending | confirmed | rejected
    @SerializedName("paymentProofUrl") val paymentProofUrl: String?,
    @SerializedName("refundStatus")    val refundStatus: String?,
    @SerializedName("attendeeStatus")  val attendeeStatus: String?,  // confirmed | rejected | attended | no_show
    @SerializedName("seatNumber")      val seatNumber: String?,
    @SerializedName("ticketToken")     val ticketToken: String?,
    @SerializedName("registeredAt")    val registeredAt: String?
)

data class ApprovePaymentRequest(
    @SerializedName("seatNumber") val seatNumber: String? = null
)

data class RejectPaymentRequest(
    val reason: String
)

data class AssignSeatRequest(
    @SerializedName("seatNumber") val seatNumber: String
)
```

---

## REPOSITORIES

### Repositories.kt
**Path:** `app/src/main/java/com/hangout/app/repository/Repositories.kt`

```kotlin
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
```

### EventRepository.kt
**Path:** `app/src/main/java/com/hangout/app/repository/EventRepository.kt`

```kotlin
package com.hangout.app.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.AppCache

class EventRepository(private val context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    // ── Attending events ──────────────────────────────────────────────────

    suspend fun getAttendingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.ATTENDING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val r = api.getAttendingEvents()
            if (r.isSuccessful) {
                val body = r.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load attending events")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    // ── RSVP — bust attending cache after any RSVP action ─────────────────

    suspend fun rsvpEvent(eventId: Long): Result<RsvpResponse> {
        return try {
            val r = api.rsvpEvent(eventId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(r.body() ?: RsvpResponse(null, null, null, true, "RSVP'd"))
            } else {
                val msg = try {
                    org.json.JSONObject(r.errorBody()?.string() ?: "")
                        .optString("message", "RSVP failed")
                } catch (_: Exception) { "RSVP failed" }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> {
        return try {
            val r = api.cancelRsvp(eventId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                Result.Success(r.body() ?: MessageResponse("Cancelled"))
            } else Result.Error("Cancel failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun checkRsvpStatus(eventId: Long): Result<RsvpStatusResponse> {
        return try {
            val r = api.checkRsvpStatus(eventId)
            if (r.isSuccessful)
                Result.Success(r.body() ?: RsvpStatusResponse(false, null, null))
            else Result.Error("Status check failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
```

---

## AUTHENTICATION

### AuthContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/auth/AuthContract.kt`

```kotlin
package com.hangout.app.ui.auth

interface AuthContract {
    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun onLoginSuccess(token: String, email: String, firstname: String)
        fun onRegisterSuccess()
        fun onSessionFound()
    }
    interface Presenter {
        fun checkSession()
        fun login(email: String, password: String)
        fun register(
            firstname: String,
            lastname: String,
            email: String,
            password: String,
            birthdate: String
        )
        fun detachView()
    }
}
```

### AuthModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/auth/AuthModel.kt`

```kotlin
package com.hangout.app.ui.auth

import android.content.Context
import com.hangout.app.data.LoginResponse
import com.hangout.app.data.MessageResponse
import com.hangout.app.repository.AuthRepository
import com.hangout.app.repository.Result
import com.hangout.app.utils.SessionManager

class AuthModel(context: Context) {

    private val repo    = AuthRepository(context)
    private val session = SessionManager(context)

    fun isLoggedIn(): Boolean = session.isLoggedIn()

    fun saveSession(token: String, email: String, firstname: String) {
        session.saveSession(token, email, firstname)
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return repo.login(email, password)
    }

    suspend fun register(
        firstname: String,
        lastname: String,
        email: String,
        password: String,
        birthdate: String
    ): Result<MessageResponse> {
        return repo.register(firstname, lastname, email, password, birthdate)
    }
}
```

### AuthPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/auth/AuthPresenter.kt`

```kotlin
package com.hangout.app.ui.auth

import android.content.Context
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AuthPresenter(
    private var view: AuthContract.View?,
    private val model: AuthModel
) : AuthContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun checkSession() {
        if (model.isLoggedIn()) view?.onSessionFound()
    }

    override fun login(email: String, password: String) {
        view?.showLoading(true)
        scope.launch {
            when (val result = model.login(email, password)) {
                is Result.Success -> {
                    model.saveSession(
                        result.data.token,
                        result.data.email,
                        result.data.firstname
                    )
                    view?.showLoading(false)
                    view?.onLoginSuccess(
                        result.data.token,
                        result.data.email,
                        result.data.firstname
                    )
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun register(
        firstname: String,
        lastname: String,
        email: String,
        password: String,
        birthdate: String
    ) {
        view?.showLoading(true)
        scope.launch {
            when (val result = model.register(firstname, lastname, email, password, birthdate)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.onRegisterSuccess()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### AuthActivity.kt
**Path:** `app/src/main/java/com/hangout/app/ui/auth/AuthActivity.kt`

```kotlin
package com.hangout.app.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.hangout.app.R
import com.hangout.app.databinding.ActivityAuthBinding
import com.hangout.app.ui.nav.NavActivity
import com.hangout.app.utils.color
import com.hangout.app.utils.getValue
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import java.util.Calendar

class AuthActivity : AppCompatActivity(), AuthContract.View {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var presenter: AuthContract.Presenter

    private var selectedBirthdate: String = ""
    private var dotIndex = 0
    private val dotHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val dotRunnable = object : Runnable {
        override fun run() {
            cycleDots()
            dotHandler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding   = ActivityAuthBinding.inflate(layoutInflater)
        presenter = AuthPresenter(this, AuthModel(this))
        setContentView(binding.root)

        presenter.checkSession()

        showSignIn()
        dotHandler.post(dotRunnable)
        setupTabSwitching()
        setupDatePicker()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        dotHandler.removeCallbacks(dotRunnable)
        presenter.detachView()
    }

    // ── AuthContract.View ──────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
        binding.btnSignIn.isEnabled        = !show
        binding.btnCreateAccount.isEnabled = !show
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.setTextColor(color(R.color.red_accent))
        binding.tvError.show()
    }

    override fun showSuccess(message: String) {
        binding.tvError.text = message
        binding.tvError.setTextColor(color(R.color.success_green))
        binding.tvError.show()
    }

    override fun onSessionFound() {
        goToHome()
    }

    override fun onLoginSuccess(token: String, email: String, firstname: String) {
        toast("Welcome back, $firstname!")
        goToHome()
    }

    override fun onRegisterSuccess() {
        showSuccess("Account created! Please sign in.")
        binding.root.postDelayed({
            showSignIn()
            binding.etSignInEmail.setText(binding.etRegisterEmail.text)
        }, 1500)
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun goToHome() {
        startActivity(Intent(this, NavActivity::class.java))
        finish()
    }

    // ── Tab Switching ──────────────────────────────────────────────────────

    private fun setupTabSwitching() {
        binding.btnTabSignIn.setOnClickListener   { showSignIn()   }
        binding.btnTabRegister.setOnClickListener { showRegister() }
    }

    private fun showSignIn() {
        binding.layoutSignInForm.show()
        binding.layoutRegisterForm.hide()
        binding.tvError.hide()
        binding.btnTabSignIn.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabSignIn.setTextColor(color(R.color.text_primary))
        binding.btnTabRegister.setBackgroundResource(android.R.color.transparent)
        binding.btnTabRegister.setTextColor(color(R.color.text_muted))
    }

    private fun showRegister() {
        binding.layoutRegisterForm.show()
        binding.layoutSignInForm.hide()
        binding.tvError.hide()
        binding.btnTabRegister.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabRegister.setTextColor(color(R.color.text_primary))
        binding.btnTabSignIn.setBackgroundResource(android.R.color.transparent)
        binding.btnTabSignIn.setTextColor(color(R.color.text_muted))
    }

    // ── Date Picker ────────────────────────────────────────────────────────

    private fun setupDatePicker() {
        val showPicker = {
            val cal = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
            DatePickerDialog(this,
                { _, year, month, day ->
                    val mm = String.format("%02d", month + 1)
                    val dd = String.format("%02d", day)
                    binding.etBirthdate.setText("$mm/$dd/$year")
                    selectedBirthdate = "$year-$mm-$dd"
                    binding.tilBirthdate.error = null
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        binding.etBirthdate.setOnClickListener         { showPicker() }
        binding.tilBirthdate.setEndIconOnClickListener { showPicker() }
    }

    // ── Click Listeners ────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            binding.tvError.hide()
            val email    = binding.etSignInEmail.getValue()
            val password = binding.etSignInPassword.getValue()
            var valid    = true

            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilSignInEmail.error = "Valid email required"; valid = false
            } else binding.tilSignInEmail.error = null

            if (password.isBlank()) {
                binding.tilSignInPassword.error = "Password required"; valid = false
            } else binding.tilSignInPassword.error = null

            if (valid) presenter.login(email, password)
        }

        binding.btnCreateAccount.setOnClickListener {
            binding.tvError.hide()
            if (validateRegisterForm()) {
                presenter.register(
                    firstname = binding.etFirstName.getValue(),
                    lastname  = binding.etLastName.getValue(),
                    email     = binding.etRegisterEmail.getValue(),
                    password  = binding.etRegisterPassword.getValue(),
                    birthdate = selectedBirthdate
                )
            }
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────

    private fun validateRegisterForm(): Boolean {
        var valid     = true
        val firstname = binding.etFirstName.getValue()
        val lastname  = binding.etLastName.getValue()
        val email     = binding.etRegisterEmail.getValue()
        val password  = binding.etRegisterPassword.getValue()
        val confirm   = binding.etConfirmPassword.getValue()

        if (firstname.isBlank()) { binding.etFirstName.error = "Required"; valid = false }
        if (lastname.isBlank())  { binding.etLastName.error  = "Required"; valid = false }

        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etRegisterEmail.error = "Valid email required"; valid = false
        }
        if (selectedBirthdate.isBlank()) {
            binding.tilBirthdate.error = "Required"; valid = false
        }

        val pwRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")
        if (!pwRegex.matches(password)) {
            showError("Password must be 8+ chars with upper, lower, number & special char")
            valid = false
        }
        if (confirm != password) {
            binding.etConfirmPassword.error = "Passwords do not match"; valid = false
        }
        return valid
    }

    // ── Dot Animation ──────────────────────────────────────────────────────

    private fun cycleDots() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { index, view ->
            view.animate().cancel()
            if (index == dotIndex) {
                view.setBackgroundResource(R.drawable.dot_active)
                view.alpha = 1f
                view.animate().scaleX(1.5f).scaleY(1.5f).setDuration(900).withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
                }.start()
            } else {
                view.setBackgroundResource(R.drawable.dot_inactive)
                view.animate().scaleX(1f).scaleY(1f).alpha(0.35f).setDuration(500).start()
            }
        }
        dotIndex = (dotIndex + 1) % 3
    }
}
```

---

## NAVIGATION

### NavActivity.kt
**Path:** `app/src/main/java/com/hangout/app/ui/nav/NavActivity.kt`

```kotlin
package com.hangout.app.ui.nav

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.databinding.ActivityMainBinding
import com.hangout.app.ui.createevent.CreateEventActivity
import com.hangout.app.ui.discover.DiscoverFragment
import com.hangout.app.ui.home.HomeFragment
import com.hangout.app.ui.myhangouts.MyHangoutsFragment
import com.hangout.app.ui.profile.ProfileFragment

class NavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.menu.getItem(2).isEnabled = false
        binding.bottomNavigation.background = null

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { replaceFragment(HomeFragment()); true }
                R.id.nav_discover -> { replaceFragment(DiscoverFragment()); true }
                R.id.nav_hangouts -> { replaceFragment(MyHangoutsFragment());true }
                R.id.nav_profile  -> { replaceFragment(ProfileFragment()); true }
                else -> false
            }
        }

        binding.fabCreate.setOnClickListener {
            startActivity(android.content.Intent(this, CreateEventActivity::class.java))
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
```

---

## HOME SCREEN

### HomeContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/home/HomeContract.kt`

```kotlin
package com.hangout.app.ui.home

import com.hangout.app.data.*

interface HomeContract {
    interface View {
        fun showProfile(profile: UserProfile)
        fun showStats(stats: UserStats)
        fun showHostingEvents(events: List<EventItem>)
        fun showTodayEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
    }
    interface Presenter {
        fun loadAll()
        fun detachView()
    }
}
```

### HomeModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/home/HomeModel.kt`

```kotlin
package com.hangout.app.ui.home

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import com.hangout.app.utils.AppCache

class HomeModel(context: Context) {

    private val userRepo = UserRepository(context)
    private val api      = RetrofitClient.getApiService(context)
    private val gson     = Gson()

    suspend fun getProfile(): Result<UserProfile> = userRepo.getProfile()

    suspend fun getStats(): Result<UserStats> = userRepo.getStats()

    suspend fun getHostingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.HOSTING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val response = api.getHostingEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else
                Result.Error("Failed to load hosting events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getTodayEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.TODAY_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val response = api.getTodayEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else
                Result.Error("Failed to load today's events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
```

### HomePresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/home/HomePresenter.kt`

```kotlin
package com.hangout.app.ui.home

import android.content.Context
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomePresenter(
    private var view: HomeContract.View?,
    private val model: HomeModel
) : HomeContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getProfile()) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> view?.showError(r.message)
            }
            when (val r = model.getStats()) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            when (val r = model.getHostingEvents()) {
                is Result.Success -> view?.showHostingEvents(r.data)
                is Result.Error   -> view?.showHostingEvents(emptyList())
            }
            when (val r = model.getTodayEvents()) {
                is Result.Success -> view?.showTodayEvents(r.data)
                is Result.Error   -> view?.showTodayEvents(emptyList())
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### HomeFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/home/HomeFragment.kt`

```kotlin
package com.hangout.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentHomeBinding
import com.hangout.app.databinding.ItemEventCardHorizontalBinding
import com.hangout.app.data.*
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.toast

class HomeFragment : Fragment(), HomeContract.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HomeContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = HomePresenter(this, HomeModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadAll()

        binding.ivNotifications.setOnClickListener {
            toast("No new notifications")
        }
        binding.tvManageHosting.setOnClickListener {
            toast("Opening My HangOuts...")
        }
    }

    // ── HomeContract.View ──────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {}

    override fun showError(message: String) {
        toast(message)
    }

    override fun showProfile(profile: UserProfile) {
        binding.tvUserName.text = "${profile.firstname}!"
    }

    override fun showStats(stats: UserStats) {
        binding.tvStatHosting.text   = stats.hostingCount.toString()
        binding.tvStatAttending.text = stats.attendingCount.toString()
        binding.tvStatAttendees.text = stats.totalAttendees.toString()
    }

    override fun showHostingEvents(events: List<EventItem>) {
        binding.layoutHostingContainer.removeAllViews()
        if (events.isEmpty()) {
            binding.layoutHostingContainer.addView(
                emptyText("No events yet — create your first HangOut!")
            )
        } else {
            events.forEach { event ->
                val card = ItemEventCardHorizontalBinding.inflate(
                    layoutInflater, binding.layoutHostingContainer, false
                )
                bindEventCard(card, event)
                binding.layoutHostingContainer.addView(card.root)
            }
        }
    }

    override fun showTodayEvents(events: List<EventItem>) {
        binding.layoutHappeningNowContainer.removeAllViews()
        if (events.isEmpty()) {
            binding.layoutHappeningNowContainer.addView(
                emptyText("Nothing happening right now.")
            )
        } else {
            events.forEach { event ->
                val card = ItemEventCardHorizontalBinding.inflate(
                    layoutInflater, binding.layoutHappeningNowContainer, false
                )
                bindEventCard(card, event)
                binding.layoutHappeningNowContainer.addView(card.root)
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun bindEventCard(cardBinding: ItemEventCardHorizontalBinding, event: EventItem) {
        cardBinding.tvEventTitle.text    = event.title
        cardBinding.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
        cardBinding.tvEventLocation.text = event.location ?: "—"
        cardBinding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        cardBinding.tvEventFormat.text   = event.format ?: "In-Person"
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(cardBinding.ivEventImage)
        }
        cardBinding.root.setOnClickListener {
            openEventDetail(event)
        }
    }

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = { presenter.loadAll() }
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        setPadding(0, 16, 0, 16)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

---

## DISCOVER SCREEN

### DiscoverContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/discover/DiscoverContract.kt`

```kotlin
package com.hangout.app.ui.discover

import com.hangout.app.data.EventItem

interface DiscoverContract {
    interface View {
        fun showEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
    }
    interface Presenter {
        fun loadEvents(search: String = "", filter: String = "")
        fun detachView()
    }
}
```

### DiscoverModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/discover/DiscoverModel.kt`

```kotlin
package com.hangout.app.ui.discover

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.EventItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class DiscoverModel(context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    suspend fun getDiscoverEvents(search: String, filter: String, forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.discoverSearch(search, filter)
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val response = api.getDiscoverEvents(search, filter)
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else
                Result.Error("Failed to load events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
```

### DiscoverPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/discover/DiscoverPresenter.kt`

```kotlin
package com.hangout.app.ui.discover

import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DiscoverPresenter(
    private var view: DiscoverContract.View?,
    private val model: DiscoverModel
) : DiscoverContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadEvents(search: String, filter: String) {
        view?.showLoading(true)
        scope.launch {
            when (val result = model.getDiscoverEvents(search, filter)) {
                is Result.Success -> view?.showEvents(result.data)
                is Result.Error   -> view?.showError(result.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### DiscoverFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/discover/DiscoverFragment.kt`

```kotlin
package com.hangout.app.ui.discover

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.utils.EventHolder
import com.hangout.app.databinding.FragmentDiscoverBinding
import com.hangout.app.databinding.ItemEventCardBinding
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.utils.toast

class DiscoverFragment : Fragment(), DiscoverContract.View {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: DiscoverContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = DiscoverPresenter(this, DiscoverModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadEvents()

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                presenter.loadEvents(search = binding.etSearch.text.toString())
                true
            } else false
        }
    }

    // ── DiscoverContract.View ──────────────────────────────────────────────

    override fun showLoading(show: Boolean) {}

    override fun showError(message: String) {
        toast(message)
        showEvents(emptyList())
    }

    override fun showEvents(events: List<EventItem>) {
        binding.layoutDiscoverEventsContainer.removeAllViews()
        if (events.isEmpty()) {
            binding.layoutDiscoverEventsContainer.addView(emptyText())
            return
        }
        events.forEach { event ->
            val card = ItemEventCardBinding.inflate(
                layoutInflater, binding.layoutDiscoverEventsContainer, false
            )
            bindEventCard(card, event)
            binding.layoutDiscoverEventsContainer.addView(card.root)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun bindEventCard(card: ItemEventCardBinding, event: EventItem) {
        card.tvEventTitle.text    = event.title
        card.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
        card.tvEventLocation.text = event.location ?: "—"
        card.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        card.tvEventFormat.text   = event.format ?: "In-Person"
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(card.ivEventImage)
        }
        card.root.setOnClickListener {
            openEventDetail(event)
        }
    }

    private fun openEventDetail(event: EventItem) {
        val currentSearch = binding.etSearch.text.toString()
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = {
                if (_binding != null) {
                    presenter.loadEvents(search = currentSearch)
                }
            }
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun emptyText() = TextView(requireContext()).apply {
        text = "No events found. Try a different search."
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 48, 0, 48)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

---

## MY HANGOUTS SCREEN

### MyHangoutContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/myhangouts/MyHangoutContract.kt`

```kotlin
package com.hangout.app.ui.myhangouts

import com.hangout.app.data.EventItem

interface MyHangoutsContract {

    interface View {
        fun showAttendingEvents(events: List<EventItem>)
        fun showHostingEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun onCancelSuccess(eventId: Long)
    }

    interface Presenter {
        fun loadAttending()
        fun loadHosting()
        fun cancelRsvp(eventId: Long)
        fun detachView()
    }
}
```

### MyHangoutsModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/myhangouts/MyHangoutsModel.kt`

```kotlin
package com.hangout.app.ui.myhangouts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.EventItem
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class MyHangoutsModel(context: Context) {

    private val eventRepo = EventRepository(context)
    private val api       = RetrofitClient.getApiService(context)
    private val gson      = Gson()

    suspend fun getAttendingEvents(): Result<List<EventItem>> =
        eventRepo.getAttendingEvents()

    suspend fun getHostingEvents(forceRefresh: Boolean = false): Result<List<EventItem>> {
        val key = AppCache.Keys.HOSTING_EVENTS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<EventItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val response = api.getHostingEvents()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, gson.toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else
                Result.Error("Failed to load hosting events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> =
        eventRepo.cancelRsvp(eventId)
}
```

### MyHangoutsPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/myhangouts/MyHangoutsPresenter.kt`

```kotlin
package com.hangout.app.ui.myhangouts

import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyHangoutsPresenter(
    private var view: MyHangoutsContract.View?,
    private val model: MyHangoutsModel
) : MyHangoutsContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAttending() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getAttendingEvents()) {
                is Result.Success -> view?.showAttendingEvents(r.data)
                is Result.Error   -> view?.showError(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun loadHosting() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getHostingEvents()) {
                is Result.Success -> view?.showHostingEvents(r.data)
                is Result.Error   -> view?.showError(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun cancelRsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.cancelRsvp(eventId)) {
                is Result.Success -> view?.onCancelSuccess(eventId)
                is Result.Error   -> view?.showError(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### MyHangoutsFragment.kt (Partial - View Logic)
**Path:** `app/src/main/java/com/hangout/app/ui/myhangouts/MyHangoutsFragment.kt`

```kotlin
package com.hangout.app.ui.myhangouts

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentMyHangoutsBinding
import com.hangout.app.databinding.ItemAttendingCardBinding
import com.hangout.app.ui.attendingdashboard.AttendingDashboardFragment
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.ui.hostdashboard.HostDashboardFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.color
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class MyHangoutsFragment : Fragment(), MyHangoutsContract.View {

    private var _binding: FragmentMyHangoutsBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: MyHangoutsContract.Presenter

    private var showingHosting = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = MyHangoutsPresenter(this, MyHangoutsModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyHangoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        showHostingTab()
    }

    // ── Tab setup ──────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.btnTabHosting.setOnClickListener {
            if (!showingHosting) showHostingTab()
        }
        binding.btnTabAttending.setOnClickListener {
            if (showingHosting) showAttendingTab()
        }
    }

    private fun showHostingTab() {
        showingHosting = true
        binding.btnTabHosting.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabHosting.setTextColor(color(R.color.white))
        binding.btnTabAttending.setBackgroundResource(android.R.color.transparent)
        binding.btnTabAttending.setTextColor(color(R.color.text_muted))
        presenter.loadHosting()
    }

    private fun showAttendingTab() {
        showingHosting = false
        binding.btnTabAttending.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabAttending.setTextColor(color(R.color.white))
        binding.btnTabHosting.setBackgroundResource(android.R.color.transparent)
        binding.btnTabHosting.setTextColor(color(R.color.text_muted))
        presenter.loadAttending()
    }

    // ── MyHangoutsContract.View ────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showError(message: String) {
        toast(message)
    }

    override fun showAttendingEvents(events: List<EventItem>) {
        binding.layoutEventsContainer.removeAllViews()
        binding.layoutEventsContainer.addView(binding.progressBar)

        if (events.isEmpty()) {
            binding.layoutEventsContainer.addView(emptyText("You haven't RSVP'd to any HangOuts yet."))
            return
        }

        events.forEach { event -> binding.layoutEventsContainer.addView(buildAttendingCard(event)) }
    }

    override fun showHostingEvents(events: List<EventItem>) {
        binding.layoutEventsContainer.removeAllViews()

        if (events.isEmpty()) {
            binding.layoutEventsContainer.addView(emptyText("You haven't created any HangOuts yet."))
            return
        }

        // Show all events — drafts included
        events.forEach { event ->
            binding.layoutEventsContainer.addView(buildHostingCard(event))
        }
    }

    override fun onCancelSuccess(eventId: Long) {
        toast("RSVP cancelled.")
        presenter.loadAttending()
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private fun buildAttendingCard(event: EventItem): View {
        val card = ItemAttendingCardBinding.inflate(layoutInflater, binding.layoutEventsContainer, false)

        card.tvEventTitle.text = event.title ?: "Untitled"
        card.tvDate.text       = "${event.date ?: "—"} • ${event.startTime ?: event.time ?: "—"}"
        card.tvLocation.text   = if (event.location.isNullOrBlank()) "Virtual / TBD" else event.location
        card.tvPrice.text      = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        val status = deriveAttendingStatus(event)
        applyStatusStyle(card, event, status)

        // Ticket info — show only when confirmed
        if (status == AttendingStatus.CONFIRMED) {
            card.layoutTicketInfo.show()
            card.tvTicketNumber.text = event.ticketNumber ?: "TKT-${event.id}"
            card.tvSeatNumber.text   = event.seatNumber ?: "Open"
        } else {
            card.layoutTicketInfo.hide()
        }

        // Cancel button — only for active, non-cancelled, non-rejected RSVPs
        val canCancel = status == AttendingStatus.CONFIRMED || status == AttendingStatus.PENDING
        card.btnCancelRsvp.showIf(canCancel)
        card.btnCancelRsvp.setOnClickListener {
            event.id?.let { id -> confirmCancel(id) }
        }

        card.btnViewDetails.setOnClickListener {
            EventHolder.currentEvent = event
            val fragment = AttendingDashboardFragment.newInstance(
                onBack = { presenter.loadAttending() }
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        return card.root
    }

    private fun buildHostingCard(event: EventItem): View {
        val card = ItemAttendingCardBinding.inflate(layoutInflater, binding.layoutEventsContainer, false)

        card.tvEventTitle.text = event.title ?: "Untitled"
        card.tvDate.text       = "${event.date ?: "—"} • ${event.startTime ?: event.time ?: "—"}"
        card.tvLocation.text   = if (event.location.isNullOrBlank()) "Virtual / TBD" else event.location
        card.tvPrice.text      = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        val isDraft     = event.isDraft == true
        val isCancelled = event.eventStatus == "cancelled"
        val isCompleted = event.eventStatus == "completed"

        card.tvStatusBadge.text = when {
            isDraft     -> "Draft"
            isCancelled -> "Cancelled"
            isCompleted -> "Completed"
            else        -> "Published"
        }

        val badgeColor = when {
            isDraft     -> ContextCompat.getColor(requireContext(), R.color.yellow_accent)
            isCancelled -> ContextCompat.getColor(requireContext(), R.color.red_accent)
            isCompleted -> ContextCompat.getColor(requireContext(), R.color.text_muted)
            else        -> ContextCompat.getColor(requireContext(), R.color.success_green)
        }
        card.tvStatusBadge.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        card.tvStatusBadge.setTextColor(badgeColor)

        card.viewStatusStripe.setBackgroundColor(
            when {
                isDraft     -> ContextCompat.getColor(requireContext(), R.color.yellow_accent)
                isCancelled -> ContextCompat.getColor(requireContext(), R.color.red_accent)
                isCompleted -> ContextCompat.getColor(requireContext(), R.color.text_muted)
                else        -> ContextCompat.getColor(requireContext(), R.color.purple_main)
            }
        )

        card.layoutTicketInfo.hide()
        card.btnCancelRsvp.hide()

        card.btnViewDetails.text = if (isDraft) "Edit Draft" else "Manage"
        card.btnViewDetails.setOnClickListener {
            if (isDraft) {
                startActivity(
                    android.content.Intent(requireContext(),
                        com.hangout.app.ui.createevent.CreateEventActivity::class.java)
                )
            } else {
                EventHolder.currentEvent = event
                val fragment = HostDashboardFragment.newInstance(
                    onBack = { presenter.loadHosting() }
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        return card.root
    }

    // ── Status helpers ─────────────────────────────────────────────────────

    private enum class AttendingStatus {
        CONFIRMED, PENDING, REJECTED, CANCELLED, COMPLETED
    }

    private fun deriveAttendingStatus(event: EventItem): AttendingStatus {
        val rsvpStatus     = event.status
        val paymentStatus  = event.rsvpPaymentStatus
        val attendeeStatus = event.attendeeStatus
        val eventStatus    = event.eventStatus

        return when {
            rsvpStatus    == "cancelled"  -> AttendingStatus.CANCELLED
            attendeeStatus == "rejected"  -> AttendingStatus.REJECTED
            paymentStatus  == "rejected"  -> AttendingStatus.REJECTED
            eventStatus    == "completed" -> AttendingStatus.COMPLETED
            paymentStatus  == "pending"   -> AttendingStatus.PENDING
            rsvpStatus     == "confirmed" -> AttendingStatus.CONFIRMED
            paymentStatus  == "confirmed" -> AttendingStatus.CONFIRMED
            else                          -> AttendingStatus.CONFIRMED
        }
    }

    private fun applyStatusStyle(
        card: ItemAttendingCardBinding,
        event: EventItem,
        status: AttendingStatus
    ) {
        val ctx = requireContext()
        when (status) {
            AttendingStatus.CONFIRMED -> {
                card.tvStatusBadge.text = "Confirmed ✓"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.success_green))
            }
            AttendingStatus.PENDING -> {
                card.tvStatusBadge.text = "Pending"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
            }
            AttendingStatus.REJECTED -> {
                card.tvStatusBadge.text = "Rejected"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.red_accent))
            }
            AttendingStatus.CANCELLED -> {
                card.tvStatusBadge.text = "Cancelled"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }
            AttendingStatus.COMPLETED -> {
                card.tvStatusBadge.text = "Completed"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.purple_light))
            }
        }
        // Clear badge background so only the text color shows
        card.tvStatusBadge.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    // ── Actions ────────────────────────────────────────────────────────────

    private fun confirmCancel(eventId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel RSVP")
            .setMessage("Are you sure you want to cancel your spot?")
            .setPositiveButton("Yes, Cancel") { _, _ -> presenter.cancelRsvp(eventId) }
            .setNegativeButton("Keep RSVP", null)
            .show()
    }

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = {
                if (showingHosting) presenter.loadHosting()
                else presenter.loadAttending()
            }
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 64, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

---

## ATTENDING DASHBOARD

### AttendingDashboardContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/attendingdashboard/AttendingDashboardContract.kt`

```kotlin
package com.hangout.app.ui.attendingdashboard

import com.hangout.app.data.EventItem

interface AttendingDashboardContract {

    interface View {
        fun showEvent(event: EventItem)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onCancelSuccess()
        fun onRefundRequested()
        fun onRefundAcknowledged()
    }

    interface Presenter {
        fun loadEvent(event: EventItem)
        fun cancelRsvp(eventId: Long)
        fun requestRefund(eventId: Long, reason: String)
        fun acknowledgeRefund(eventId: Long)
        fun detachView()
    }
}
```

### AttendingDashboardFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/attendingdashboard/AttendingDashboardFragment.kt`

```kotlin
package com.hangout.app.ui.attendingdashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentAttendingDashboardBinding
import com.hangout.app.ui.paymentproof.PaymentProofBottomSheet
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class AttendingDashboardFragment : Fragment(), AttendingDashboardContract.View {

    private var _binding: FragmentAttendingDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: AttendingDashboardContract.Presenter
    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            AttendingDashboardFragment().apply { onBackCallback = onBack }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = AttendingDashboardPresenter(this, AttendingDashboardModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendingDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: return

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { presenter.loadEvent(it) }
    }

    // ── AttendingDashboardContract.View ───────────────────────────────────

    override fun showEvent(event: EventItem) {
        this.event = event

        // ── Event info ────────────────────────────────────────────────────
        binding.tvEventTitle.text    = event.title ?: "Untitled"
        binding.tvEventDate.text     = "📅 ${event.date ?: "—"}  ·  ${formatTime(event)}"
        binding.tvEventLocation.text = "📍 ${event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"}"
        binding.tvEventFormat.text   = event.format ?: "In-Person"
        binding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "Free"
        else "₱${event.price?.toInt()}"

        // ── Status banner ─────────────────────────────────────────────────
        val (statusLabel, statusColor) = resolveStatusDisplay(event)
        binding.tvStatusLabel.text = statusLabel
        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
        binding.viewStatusStripe.setBackgroundColor(
            ContextCompat.getColor(requireContext(), statusColor)
        )

        // ── Ticket info (confirmed only) ──────────────────────────────────
        val isConfirmed = event.rsvpPaymentStatus == "confirmed" ||
                (event.status == "confirmed" && event.rsvpPaymentStatus != "pending")
        binding.layoutTicketInfo.showIf(isConfirmed)
        if (isConfirmed) {
            binding.tvTicketNumber.text = event.ticketNumber ?: "TKT-${event.id}"
            binding.tvSeatNumber.text   = event.seatNumber?.ifBlank { "Open Seating" } ?: "Open Seating"
        }

        // ── Virtual link ──────────────────────────────────────────────────
        val hasVirtualLink = !event.virtualLink.isNullOrBlank()
        binding.layoutVirtualLink.showIf(hasVirtualLink && isConfirmed)
        binding.btnJoinMeeting.setOnClickListener {
            event.virtualLink?.let { link ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }

        // ── Payment status section (paid events) ──────────────────────────
        val isPaidEvent = (event.price ?: 0.0) > 0.0
        binding.layoutPaymentSection.showIf(isPaidEvent)
        if (isPaidEvent) {
            val payLabel = when (event.rsvpPaymentStatus) {
                "pending"   -> "⏳ Pending Host Approval"
                "confirmed" -> "✓ Payment Approved"
                "rejected"  -> "✗ Payment Rejected"
                else        -> "Not yet submitted"
            }
            val payColor = when (event.rsvpPaymentStatus) {
                "pending"   -> R.color.yellow_accent
                "confirmed" -> R.color.success_green
                "rejected"  -> R.color.red_accent
                else        -> R.color.text_muted
            }
            binding.tvPaymentStatus.text = payLabel
            binding.tvPaymentStatus.setTextColor(
                ContextCompat.getColor(requireContext(), payColor)
            )
        }

        // ── Refund status ─────────────────────────────────────────────────
        val hasRefund = !event.refundStatus.isNullOrBlank() &&
                event.refundStatus != "none"
        binding.layoutRefundSection.showIf(hasRefund)
        if (hasRefund) {
            val refundLabel = when (event.refundStatus) {
                "pending"                -> "Refund Requested"
                "waiting_acknowledgement"-> "Refund Processed — tap to acknowledge"
                "completed"              -> "Refund Completed ✓"
                "rejected"               -> "Refund Rejected"
                else                     -> event.refundStatus ?: ""
            }
            val refundColor = when (event.refundStatus) {
                "pending"                -> R.color.yellow_accent
                "waiting_acknowledgement"-> R.color.purple_light
                "completed"              -> R.color.success_green
                "rejected"               -> R.color.red_accent
                else                    -> R.color.text_muted
            }
            binding.tvRefundStatus.text = refundLabel
            binding.tvRefundStatus.setTextColor(
                ContextCompat.getColor(requireContext(), refundColor)
            )
        }

        // ── Action buttons ────────────────────────────────────────────────
        setupActionButtons(event)
    }

    private fun setupActionButtons(event: EventItem) {
        val eventId        = event.id ?: return
        val isCancelled    = event.status == "cancelled"
        val isRejected     = event.attendeeStatus == "rejected" ||
                event.rsvpPaymentStatus == "rejected"
        val isConfirmed    = event.rsvpPaymentStatus == "confirmed" ||
                (event.status == "confirmed" && event.rsvpPaymentStatus != "pending")
        val isPending      = event.rsvpPaymentStatus == "pending"
        val noProof        = event.rsvpPaymentStatus == null && (event.price ?: 0.0) > 0.0
        val noRefundPolicy = event.noRefundPolicy == true
        val refundStatus   = event.refundStatus
        val isEventActive  = event.eventStatus == "active" || event.eventStatus == null

        // Upload proof — show when paid + no proof yet or payment rejected
        val canUploadProof = (event.price ?: 0.0) > 0.0 &&
                !isCancelled && !isRejected &&
                (noProof || event.rsvpPaymentStatus == "rejected")
        binding.btnUploadProof.showIf(canUploadProof)
        binding.btnUploadProof.setOnClickListener {
            PaymentProofBottomSheet.newInstance(
                event     = event,
                onSuccess = {
                    toast("Proof submitted! Awaiting host approval.")
                    parentFragmentManager.popBackStack()
                    onBackCallback?.invoke()
                }
            ).show(parentFragmentManager, "payment_proof")
        }

        // Cancel RSVP — show when not already cancelled/rejected and event is active
        val canCancel = !isCancelled && !isRejected && isEventActive &&
                refundStatus.isNullOrBlank()
        binding.btnCancelRsvp.showIf(canCancel)
        binding.btnCancelRsvp.setOnClickListener {
            promptCancelRsvp(eventId)
        }

        // Request refund — confirmed + paid + not already refunded + no-refund policy off
        val canRefund = isConfirmed && (event.price ?: 0.0) > 0.0 &&
                !noRefundPolicy && isEventActive &&
                (refundStatus.isNullOrBlank() || refundStatus == "rejected")
        binding.btnRequestRefund.showIf(canRefund)
        binding.btnRequestRefund.setOnClickListener {
            promptRequestRefund(eventId)
        }

        // Acknowledge refund — only when host has processed it
        val canAcknowledge = refundStatus == "waiting_acknowledgement"
        binding.btnAcknowledgeRefund.showIf(canAcknowledge)
        binding.btnAcknowledgeRefund.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Acknowledge Refund")
                .setMessage("Confirm that you have received your refund?")
                .setPositiveButton("Yes, I received it") { _, _ ->
                    presenter.acknowledgeRefund(eventId)
                }
                .setNegativeButton("Not yet", null)
                .show()
        }
    }

    // ── Contract callbacks ───────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) = toast(message)

    override fun onCancelSuccess() {
        toast("RSVP cancelled.")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    override fun onRefundRequested() {
        toast("Refund requested. The host will review it.")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    override fun onRefundAcknowledged() {
        toast("Refund acknowledged. Thank you!")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    private fun promptCancelRsvp(eventId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel RSVP")
            .setMessage("Are you sure you want to cancel your spot? This cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ -> presenter.cancelRsvp(eventId) }
            .setNegativeButton("Keep RSVP", null)
            .show()
    }

    private fun promptRequestRefund(eventId: Long) {
        val input = EditText(requireContext()).apply {
            hint = "Reason for refund request"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Request Refund")
            .setMessage("Please provide a reason:")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val reason = input.text.toString().trim()
                    .ifBlank { "Refund requested by attendee." }
                presenter.requestRefund(eventId, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun resolveStatusDisplay(event: EventItem): Pair<String, Int> = when {
        event.status         == "cancelled"              -> "Cancelled"             to R.color.text_muted
        event.attendeeStatus == "rejected"               -> "Rejected by Host"      to R.color.red_accent
        event.rsvpPaymentStatus == "rejected"            -> "Payment Rejected"      to R.color.red_accent
        event.attendeeStatus == "attended"               -> "Attended ✓"            to R.color.success_green
        event.eventStatus    == "completed"              -> "Event Completed"       to R.color.purple_light
        event.rsvpPaymentStatus == "pending"             -> "Pending Payment Approval" to R.color.yellow_accent
        event.rsvpPaymentStatus == "confirmed"           -> "Confirmed ✓"           to R.color.success_green
        event.status         == "confirmed"              -> "Confirmed ✓"           to R.color.success_green
        event.refundStatus   == "waiting_acknowledgement"-> "Refund Processed"      to R.color.purple_light
        event.refundStatus   == "completed"              -> "Refund Completed"      to R.color.success_green
        else                                             -> "Registered"            to R.color.purple_main
    }

    private fun formatTime(event: EventItem): String {
        val start = event.startTime ?: event.time ?: return "Time TBD"
        val end   = event.endTime
        return if (!end.isNullOrBlank()) "$start – $end" else start
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

### DigitalTicketFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/ticket/DigitatTicketFragment.kt`

```kotlin
package com.hangout.app.ui.ticket

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentDigitalTicketBinding
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast

class DigitalTicketFragment : Fragment() {

    private var _binding: FragmentDigitalTicketBinding? = null
    private val binding get() = _binding!!

    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            DigitalTicketFragment().apply { onBackCallback = onBack }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDigitalTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: run {
            toast("No ticket data found.")
            parentFragmentManager.popBackStack()
            return
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { bindTicket(it) }
    }

    private fun bindTicket(e: EventItem) {
        if (!e.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(e.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.cover_photo_bg)
                .into(binding.ivCoverImage)
            binding.ivCoverImage.show()
        } else {
            binding.ivCoverImage.hide()
        }

        binding.tvEventTitle.text    = e.title ?: "Untitled"
        binding.tvEventDate.text     = formatDate(e)
        binding.tvEventTime.text     = formatTime(e)
        binding.tvEventLocation.text = e.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"
        binding.tvEventFormat.text   = e.format ?: "In-Person"

        val ticketNumber = e.ticketNumber ?: "TKT-${e.id}"
        binding.tvTicketNumber.text  = ticketNumber
        binding.tvSeatNumber.text    = e.seatNumber?.ifBlank { "Open Seating" } ?: "Open Seating"
        binding.tvAttendeeName.text  = "${e.hostFirstName ?: ""} ${e.hostLastName ?: ""}".trim()
            .ifBlank { "Attendee" }

        val isAttended = e.attendeeStatus == "attended"
        if (isAttended) {
            binding.tvCheckinStatus.text = "✓ Checked In"
            binding.tvCheckinStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success_green)
            )
            binding.viewCheckinBg.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success_green)
            )
        } else {
            binding.tvCheckinStatus.text = "Not Yet Checked In"
            binding.tvCheckinStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
        }

        val qrContent = e.ticketToken ?: ticketNumber
        generateQrCode(qrContent)?.let { bitmap ->
            binding.ivQrCode.setImageBitmap(bitmap)
        } ?: run {
            binding.tvQrFallback.show()
            binding.ivQrCode.hide()
        }

        binding.tvQrLabel.text = "Scan this code at the event entrance"
    }

    private fun generateQrCode(content: String): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN          to 1,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val width  = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) 0xFFC084FC.toInt()
                        else                  0xFF13131F.toInt()
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(e: EventItem): String = e.date ?: "Date TBD"

    private fun formatTime(e: EventItem): String {
        val start = e.startTime ?: e.time ?: return "Time TBD"
        val end   = e.endTime
        return if (!end.isNullOrBlank()) {
            "${to12Hr(start)} – ${to12Hr(end)}"
        } else {
            to12Hr(start)
        }
    }

    private fun to12Hr(time24: String): String {
        return try {
            val parts = time24.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val ampm    = if (h >= 12) "PM" else "AM"
            val display = when {
                h == 0  -> 12
                h > 12  -> h - 12
                else    -> h
            }
            String.format("%d:%02d %s", display, m, ampm)
        } catch (_: Exception) { time24 }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

### AttendingDashboardModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/attendingdashboard/AttendingDashboardModel.kt`

```kotlin
package com.hangout.app.ui.attendingdashboard

import android.content.Context
import com.hangout.app.data.MessageResponse
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

class AttendingDashboardModel(context: Context) {

    private val eventRepo = EventRepository(context)
    private val api       = RetrofitClient.getApiService(context)

    suspend fun cancelRsvp(eventId: Long): Result<MessageResponse> =
        eventRepo.cancelRsvp(eventId)

    suspend fun requestRefund(eventId: Long, reason: String): Result<MessageResponse> {
        return try {
            val response = api.requestRefund(eventId, mapOf("reason" to reason))
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Refund requested"))
            else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun acknowledgeRefund(eventId: Long): Result<MessageResponse> {
        return try {
            val response = api.acknowledgeRefund(eventId)
            if (response.isSuccessful)
                Result.Success(response.body() ?: MessageResponse("Acknowledged"))
            else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    private fun parseError(body: String?): String {
        if (body == null) return "Server error"
        return try {
            org.json.JSONObject(body).optString("message", "Server error")
        } catch (_: Exception) { "Server error" }
    }
}
```

### AttendingDashboardPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/attendingdashboard/AttendingDashboardPresenter.kt`

```kotlin
package com.hangout.app.ui.attendingdashboard

import com.hangout.app.data.EventItem
import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class AttendingDashboardPresenter(
    private var view: AttendingDashboardContract.View?,
    private val model: AttendingDashboardModel
) : AttendingDashboardContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadEvent(event: EventItem) {
        view?.showEvent(event)
    }

    override fun cancelRsvp(eventId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.cancelRsvp(eventId)) {
                is Result.Success -> view?.onCancelSuccess()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun requestRefund(eventId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.requestRefund(eventId, reason)) {
                is Result.Success -> view?.onRefundRequested()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun acknowledgeRefund(eventId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.acknowledgeRefund(eventId)) {
                is Result.Success -> view?.onRefundAcknowledged()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

---

## EVENT DETAIL SCREEN

### EventDetailContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/eventdetail/EventDetailContract.kt`

```kotlin
package com.hangout.app.ui.eventdetail

import com.hangout.app.data.EventItem

interface EventDetailContract {
    interface View {
        fun showEvent(event: EventItem)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onRsvpSuccess()
        fun onRsvpRemoved()
        fun onRsvpStatusLoaded(isRsvped: Boolean, paymentStatus: String?)
    }
    interface Presenter {
        fun loadEvent(event: EventItem)
        fun checkRsvpStatus(eventId: Long)
        fun rsvp(eventId: Long)
        fun removeRsvp(eventId: Long)
        fun detachView()
    }
}
```

### EventDetailModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/eventdetail/EventDetailModel.kt`

```kotlin
package com.hangout.app.ui.eventdetail

import android.content.Context
import com.hangout.app.data.RsvpResponse
import com.hangout.app.data.RsvpStatusResponse
import com.hangout.app.data.MessageResponse
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

class EventDetailModel(context: Context) {

    private val repo = EventRepository(context)

    suspend fun checkRsvpStatus(eventId: Long): Result<RsvpStatusResponse> =
        repo.checkRsvpStatus(eventId)

    suspend fun rsvp(eventId: Long): Result<RsvpResponse> =
        repo.rsvpEvent(eventId)

    suspend fun removeRsvp(eventId: Long): Result<MessageResponse> =
        repo.cancelRsvp(eventId)
}
```

### EventDetailPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/eventdetail/EventDetailPresenter.kt`

```kotlin
package com.hangout.app.ui.eventdetail

import com.hangout.app.data.EventItem
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EventDetailPresenter(
    private var view: EventDetailContract.View?,
    private val model: EventDetailModel
) : EventDetailContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadEvent(event: EventItem) {
        view?.showEvent(event)
        // Auto-check RSVP status when event loads
        event.id?.let { checkRsvpStatus(it) }
    }

    override fun checkRsvpStatus(eventId: Long) {
        scope.launch {
            when (val r = model.checkRsvpStatus(eventId)) {
                is Result.Success -> {
                    val isRsvped = r.data.rsvped &&
                            r.data.status != "cancelled" &&
                            r.data.status != "rejected"
                    view?.onRsvpStatusLoaded(isRsvped, r.data.paymentStatus)
                }
                is Result.Error -> {
                    // Silently fail — treat as not RSVP'd
                    view?.onRsvpStatusLoaded(false, null)
                }
            }
        }
    }

    override fun rsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.rsvp(eventId)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showMessage("RSVP confirmed!")
                    view?.onRsvpSuccess()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun removeRsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.removeRsvp(eventId)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showMessage("RSVP cancelled.")
                    view?.onRsvpRemoved()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### EventDetailFragment.kt (Partial)
**Path:** `app/src/main/java/com/hangout/app/ui/eventdetail/EventDetailFragment.kt`

Note: This is a large file. Key sections:

```kotlin
package com.hangout.app.ui.eventdetail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentEventDetailBinding
import com.hangout.app.ui.paymentproof.PaymentProofBottomSheet
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast

class EventDetailFragment : Fragment(), EventDetailContract.View {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: EventDetailContract.Presenter



    private var isLiked  = false
    private var isRsvped = false

    // Callback invoked when this fragment is popped so Discover can refresh
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =  // remove event param
            EventDetailFragment().apply {
                onBackCallback = onBack
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = EventDetailPresenter(this, EventDetailModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val event = EventHolder.currentEvent ?: return
        presenter.loadEvent(event)
    }

    // ── EventDetailContract.View ───────────────────────────────────────────

    override fun showEvent(event: EventItem) {
        bindHero(event)
        bindDateLocation(event)
        bindStats(event)
        bindAvailability(event)
        bindAbout(event)
        bindHost(event)
        bindPayment(event)
        bindRsvpBar(event)
        setupClickListeners(event)
    }

    override fun showLoading(show: Boolean) {}

    override fun showMessage(message: String) {
        toast(message)
    }

    override fun onRsvpSuccess() {
        isRsvped = true
        val isPaid = (EventHolder.currentEvent?.price ?: 0.0) > 0.0
        if (isPaid) {
            // Immediately open payment proof sheet
            openPaymentProofSheet()
        } else {
            binding.btnRsvp.text = "✓  RSVP'd!"
            binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_success_bg)
        }
    }

    override fun onRsvpRemoved() {
        isRsvped = false
        binding.btnRsvp.text = "RSVP Now"
        binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
    }

    override fun onRsvpStatusLoaded(isRsvped: Boolean, paymentStatus: String?) {
        this.isRsvped = isRsvped
        when {
            paymentStatus == null && isRsvped && (EventHolder.currentEvent?.price ?: 0.0) > 0.0 -> {
                binding.btnRsvp.text = "Upload Payment Proof"
                binding.btnRsvp.isEnabled = true
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
                binding.btnRsvp.setOnClickListener { openPaymentProofSheet() }
            }
            paymentStatus == "pending" -> {
                binding.btnRsvp.text = "Awaiting Approval"
                binding.btnRsvp.isEnabled = false
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_pending_bg)
            }
            isRsvped -> {
                binding.btnRsvp.text = "✓  RSVP'd!"
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_success_bg)
            }
            else -> {
                binding.btnRsvp.text = "RSVP Now"
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
            }
        }
    }

    // ── Bind helpers ───────────────────────────────────────────────────────

    private fun bindHero(event: EventItem) {
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(binding.ivEventHero)
        }
        binding.tvFormatBadge.text = event.format ?: "In-Person"
        binding.tvEventTitle.text  = event.title
    }

    private fun bindDateLocation(event: EventItem) {
        binding.tvEventDate.text = formatDate(event.date)
        binding.tvEventTime.text = event.time ?: "TBD"

        val isVirtual = event.format?.lowercase() == "virtual"
        if (!event.location.isNullOrBlank() && !isVirtual) {
            binding.cardLocation.show()
            binding.tvEventLocation.text = event.location
        } else {
            binding.cardLocation.hide()
        }
    }

    // Add this helper anywhere in EventDetailFragment:
    private fun openPaymentProofSheet() {
        val e = EventHolder.currentEvent ?: return
        PaymentProofBottomSheet.newInstance(
            event     = e,
            onSuccess = {
                // Refresh RSVP status after proof submitted
                e.id?.let { presenter.checkRsvpStatus(it) }
            }
        ).show(parentFragmentManager, "payment_proof")
    }

    private fun bindStats(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        binding.tvStatPrice.text     = if (isPaid) "₱${event.price?.toInt()}" else "Free"
        binding.tvStatAttending.text = (event.attendeeCount ?: 0).toString()
        binding.tvStatFormat.text    = event.format ?: "In-Person"
        binding.tvStatCapacity.text  = (event.capacity ?: 0).toString()
    }

    private fun bindAvailability(event: EventItem) {
        val current = event.attendeeCount ?: 0
        val max     = (event.capacity ?: 100).coerceAtLeast(1)
        val left    = (max - current).coerceAtLeast(0)
        val pct     = (current.toFloat() / max.toFloat() * 100).toInt().coerceIn(0, 100)

        binding.tvAttendingCount.text     = "$current attending"
        binding.tvSpotsLeft.text          = "$left spots left"
        binding.progressCapacity.progress = pct
    }

    private fun bindAbout(event: EventItem) {
        binding.tvEventDescription.text = event.description ?: "No description provided."
        binding.tvSeatingType.text =
            if (event.seatingType == "reserved") "Assigned Seats" else "Open Seating"
    }

    private fun bindHost(event: EventItem) {
        val first    = event.hostFirstName ?: ""
        val last     = event.hostLastName  ?: ""
        val initials = (first.firstOrNull()?.uppercase() ?: "") +
                (last.firstOrNull()?.uppercase()  ?: "")
        binding.tvHostInitials.text = initials.ifBlank { "HO" }
        binding.tvHostName.text     = "$first $last".trim()
        binding.tvHostEmail.text    = event.hostEmail ?: ""
    }

    private fun bindPayment(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        if (isPaid) {
            binding.cardPayment.show()
            binding.tvPaymentMethod.text = when (event.paymentMethod?.lowercase()) {
                "gcash"   -> "GCash"
                "paymaya" -> "PayMaya"
                "bank"    -> "Bank Transfer"
                else      -> event.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "GCash"
            }
            binding.tvAccountNumber.text = event.accountNumber ?: "—"
        } else {
            binding.cardPayment.hide()
        }
    }

    private fun bindRsvpBar(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        val left   = ((event.capacity ?: 100) - (event.attendeeCount ?: 0)).coerceAtLeast(0)
        binding.tvRsvpPrice.text  = if (isPaid) "₱${event.price?.toInt()}" else "Free"
        binding.tvRsvpSeats.text  = if (left > 0) "$left spots remaining" else "Fully booked"
        binding.btnRsvp.isEnabled = left > 0
    }

    // ── Click listeners ────────────────────────────────────────────────────

    private fun setupClickListeners(event: EventItem) {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.btnLike.setOnClickListener {
            isLiked = !isLiked
            val color = if (isLiked)
                requireContext().getColor(R.color.red_accent)
            else
                requireContext().getColor(R.color.text_primary)
            binding.btnLike.setColorFilter(color)
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out: ${event.title}")
            }
            startActivity(Intent.createChooser(intent, "Share Event"))
        }

        binding.tvOpenMaps.setOnClickListener {
            val query = Uri.encode(event.location ?: "")
            val uri   = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.btnMessageHost.setOnClickListener {
            toast("Message host — coming soon!")
        }

        binding.btnRsvp.setOnClickListener {
            val id = event.id ?: return@setOnClickListener
            if (isRsvped) presenter.removeRsvp(id) else presenter.rsvp(id)
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun navigateBack() {
        // Fire the callback BEFORE popping so the target fragment is still
        // in the back stack and can react (e.g. trigger a refresh)
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    // Also handle the system back gesture
    override fun onStart() {
        super.onStart()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwnerLiveData.value ?: return,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            }
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "Date TBD"
        return try {
            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val outFmt = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
            val date   = sdf.parse(raw) ?: return raw
            outFmt.format(date)
        } catch (e: Exception) { raw }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

---

## PROFILE SCREEN

### ProfileContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/profile/ProfileContract.kt`

```kotlin
package com.hangout.app.ui.profile

import com.hangout.app.data.*
import java.io.File

interface ProfileContract {
    interface View {
        fun showProfile(profile: UserProfile)
        fun showStats(stats: UserStats)
        fun showPhoto(photoData: String)
        fun clearPhoto()
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onProfileUpdateSuccess()
        fun onPasswordUpdateSuccess()
    }
    interface Presenter {
        fun loadAll(forceRefresh: Boolean = false)
        fun updateProfile(firstname: String, lastname: String)
        fun updatePassword(oldPassword: String, newPassword: String)
        fun uploadPhoto(file: File)
        fun deletePhoto()
        fun clearSession()
        fun detachView()
    }
}
```

### ProfileModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/profile/ProfileModel.kt`

```kotlin
package com.hangout.app.ui.profile

import android.content.Context
import com.hangout.app.data.*
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import com.hangout.app.utils.SessionManager
import java.io.File

class ProfileModel(context: Context) {

    private val repo    = UserRepository(context)
    private val session = SessionManager(context)

    fun clearSession() = session.clearSession()

    suspend fun getProfile(forceRefresh: Boolean = false): Result<UserProfile> =
        repo.getProfile(forceRefresh)

    suspend fun getStats(forceRefresh: Boolean = false): Result<UserStats> =
        repo.getStats(forceRefresh)

    suspend fun getPhoto(forceRefresh: Boolean = false): Result<String> =
        repo.getPhoto(forceRefresh)

    suspend fun updateProfile(firstname: String, lastname: String): Result<MessageResponse> =
        repo.updateProfile(firstname, lastname)

    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<MessageResponse> =
        repo.updatePassword(oldPassword, newPassword)

    suspend fun uploadPhoto(file: File): Result<MessageResponse> =
        repo.uploadPhoto(file)

    suspend fun deletePhoto(): Result<MessageResponse> =
        repo.deletePhoto()
}
```

### ProfilePresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/profile/ProfilePresenter.kt`

```kotlin
package com.hangout.app.ui.profile

import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class ProfilePresenter(
    private var view: ProfileContract.View?,
    private val model: ProfileModel
) : ProfileContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll(forceRefresh: Boolean) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getProfile(forceRefresh)) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> view?.showMessage(r.message)
            }
            when (val r = model.getStats(forceRefresh)) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            when (val r = model.getPhoto(forceRefresh)) {
                is Result.Success -> view?.showPhoto(r.data)
                is Result.Error   -> view?.clearPhoto()
            }
            view?.showLoading(false)
        }
    }

    override fun updateProfile(firstname: String, lastname: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.updateProfile(firstname, lastname)) {
                is Result.Success -> {
                    view?.showMessage("Profile updated successfully!")
                    view?.onProfileUpdateSuccess()
                    loadAll(forceRefresh = true)
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun updatePassword(oldPassword: String, newPassword: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.updatePassword(oldPassword, newPassword)) {
                is Result.Success -> {
                    view?.showMessage("Password changed successfully!")
                    view?.onPasswordUpdateSuccess()
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun uploadPhoto(file: File) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.uploadPhoto(file)) {
                is Result.Success -> {
                    view?.showMessage("Photo updated!")
                    when (val p = model.getPhoto(forceRefresh = true)) {
                        is Result.Success -> view?.showPhoto(p.data)
                        is Result.Error   -> { }
                    }
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun deletePhoto() {
        scope.launch {
            when (val r = model.deletePhoto()) {
                is Result.Success -> {
                    view?.clearPhoto()
                    view?.showMessage("Photo removed.")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
        }
    }

    override fun clearSession() {
        model.clearSession()
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### ProfileFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/profile/ProfileFragment.kt`

```kotlin
package com.hangout.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hangout.app.R
import com.hangout.app.databinding.DialogChangePasswordBinding
import com.hangout.app.databinding.DialogEditProfileBinding
import com.hangout.app.databinding.FragmentProfileBinding
import com.hangout.app.data.*
import com.hangout.app.ui.auth.AuthActivity
import com.hangout.app.utils.AppCache
import com.hangout.app.utils.getValue
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment(), ProfileContract.View {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ProfileContract.Presenter

    private var editProfileDialog: BottomSheetDialog? = null
    private var changePasswordDialog: BottomSheetDialog? = null
    private var editProfileBinding: DialogEditProfileBinding? = null
    private var currentPhotoData: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = ProfilePresenter(this, ProfileModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadAll()

        binding.btnEditProfile.setOnClickListener  { showEditProfileDialog() }
        binding.ivCameraOverlay.setOnClickListener { showEditProfileDialog() }

        setupSettingsRow(
            binding.rowPersonalInfo.root,
            "Personal Information", "Update your details", R.drawable.ic_user
        ) { showEditProfileDialog() }

        setupSettingsRow(
            binding.rowChangePassword.root,
            "Privacy & Security", "Change password & privacy", R.drawable.ic_lock
        ) { showChangePasswordDialog() }

        setupSettingsRow(
            binding.rowSignOut.root,
            "Sign Out", "Log out of your account", R.drawable.ic_logout
        ) { signOut() }
    }

    // ── ProfileContract.View ───────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) {
        toast(message)
    }

    override fun showProfile(profile: UserProfile) {
        val fullName = "${profile.firstname} ${profile.lastname}".trim()
        binding.tvFullName.text = fullName.ifBlank { "Your Name" }
        binding.tvEmail.text    = profile.email
        val initials = (profile.firstname.firstOrNull()?.uppercase() ?: "") +
                (profile.lastname.firstOrNull()?.uppercase() ?: "")
        binding.tvInitials.text = initials.ifBlank { "YO" }
    }

    override fun showStats(stats: UserStats) {
        binding.tvHostingCount.text   = stats.hostingCount.toString()
        binding.tvAttendingCount.text = stats.attendingCount.toString()
        binding.tvAttendeesCount.text = stats.totalAttendees.toString()
    }

    override fun showPhoto(photoData: String) {
        currentPhotoData = photoData
        val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
        val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.ivAvatar.setImageBitmap(bmp)
        binding.ivAvatar.show()
        binding.tvInitials.hide()
    }

    override fun clearPhoto() {
        currentPhotoData = null
        binding.ivAvatar.hide()
        binding.tvInitials.show()
    }

    override fun onProfileUpdateSuccess() {
        editProfileDialog?.dismiss()
    }

    override fun onPasswordUpdateSuccess() {
        changePasswordDialog?.dismiss()
    }

    // ── Sign Out ───────────────────────────────────────────────────────────

    private fun signOut() {
        AppCache.bustAll(requireContext())
        presenter.clearSession()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    private fun showEditProfileDialog() {
        val db = DialogEditProfileBinding.inflate(layoutInflater)
        editProfileBinding = db

        currentPhotoData?.let { photoData ->
            val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
            db.ivPhotoPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            db.tvPhotoInitials.hide()
            db.btnRemovePhoto.show()
        } ?: run {
            db.btnRemovePhoto.hide()
        }

        db.btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        db.btnRemovePhoto.setOnClickListener {
            presenter.deletePhoto()
            db.ivPhotoPreview.setImageDrawable(null)
            db.tvPhotoInitials.show()
            db.btnRemovePhoto.hide()
        }

        db.btnSave.setOnClickListener {
            val firstname = db.etFirstName.getValue()
            val lastname  = db.etLastName.getValue()
            if (firstname.isBlank()) {
                db.tilFirstName.error = "First name is required"
                return@setOnClickListener
            }
            db.tilFirstName.error = null
            presenter.updateProfile(firstname, lastname)
        }

        db.btnClose.setOnClickListener  { editProfileDialog?.dismiss() }
        db.btnCancel.setOnClickListener { editProfileDialog?.dismiss() }

        editProfileDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme).apply {
            setContentView(db.root)
            show()
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
            bottomSheet?.elevation  = 0f
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.decorView?.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun showChangePasswordDialog() {
        val db = DialogChangePasswordBinding.inflate(layoutInflater)

        db.btnUpdate.setOnClickListener {
            val old     = db.etCurrentPassword.getValue()
            val newPw   = db.etNewPassword.getValue()
            val confirm = db.etConfirmPassword.getValue()
            var valid   = true

            if (old.isBlank())    { db.tilCurrentPassword.error = "Required"; valid = false }
            else db.tilCurrentPassword.error = null
            if (newPw.length < 6) { db.tilNewPassword.error = "Minimum 6 characters"; valid = false }
            else db.tilNewPassword.error = null
            if (newPw != confirm) { db.tilConfirmPassword.error = "Passwords do not match"; valid = false }
            else db.tilConfirmPassword.error = null

            if (valid) presenter.updatePassword(old, newPw)
        }

        db.btnClose.setOnClickListener  { changePasswordDialog?.dismiss() }
        db.btnCancel.setOnClickListener { changePasswordDialog?.dismiss() }

        changePasswordDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme).apply {
            setContentView(db.root)
            show()
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
            bottomSheet?.elevation  = 0f
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.decorView?.setBackgroundResource(android.R.color.transparent)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun setupSettingsRow(
        rowView: View, title: String, subtitle: String,
        iconRes: Int, danger: Boolean = false, onClick: () -> Unit
    ) {
        rowView.findViewById<android.widget.TextView>(R.id.tvRowTitle).apply {
            text = title
            if (danger) setTextColor(
                androidx.core.content.ContextCompat.getColor(rowView.context, R.color.red_accent)
            )
        }
        rowView.findViewById<android.widget.TextView>(R.id.tvRowSubtitle).text = subtitle
        rowView.findViewById<android.widget.ImageView>(R.id.ivRowIcon).setImageResource(iconRes)
        rowView.setOnClickListener { onClick() }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri  = data?.data ?: return
            val file = uriToFile(uri) ?: return
            presenter.uploadPhoto(file)
            editProfileBinding?.let { db ->
                db.ivPhotoPreview.setImageURI(uri)
                db.tvPhotoInitials.hide()
                db.btnRemovePhoto.show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file  = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { input.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding           = null
        editProfileBinding = null
    }
}
```

---

## CREATE EVENT SCREEN

### CreateEventModels.kt
**Path:** `app/src/main/java/com/hangout/app/data/CreateEventModels.kt`

```kotlin
package com.hangout.app.data

import com.google.gson.annotations.SerializedName

// ── Request body sent to POST /api/events ────────────────────────────────────

data class CreateEventRequest(
    val title: String,
    val description: String,
    val date: String,           // "YYYY-MM-DD"
    @SerializedName("startTime") val startTime: String,    // "HH:mm"
    @SerializedName("endTime")   val endTime: String?,     // "HH:mm" nullable
    val location: String,
    val format: String,         // "In-Person" | "Virtual" | "Hybrid"
    @SerializedName("eventType") val eventType: String,    // "free" | "paid"
    val price: Int,
    val capacity: Int,
    @SerializedName("seatingType")     val seatingType: String,      // "open" | "reserved"
    @SerializedName("paymentMethod")   val paymentMethod: String?,
    @SerializedName("accountName")     val accountName: String?,
    @SerializedName("accountNumber")   val accountNumber: String?,
    @SerializedName("virtualPlatform") val virtualPlatform: String?,
    @SerializedName("virtualLink")     val virtualLink: String?,
    @SerializedName("noRefundPolicy")  val noRefundPolicy: Boolean,
    @SerializedName("isDraft")         val isDraft: Boolean
)

// ── Response from POST /api/events ───────────────────────────────────────────

data class CreateEventResponse(
    val id: Long,
    val title: String,
    val message: String?
)

// ── Internal UI state (not sent directly) ────────────────────────────────────

data class CreateEventFormState(
    // Step 1 — Basics
    val title: String = "",
    val description: String = "",
    val coverImagePath: String? = null,   // local file URI for upload preview

    // Step 2 — Date, Time & Format
    val date: String = "",                // "YYYY-MM-DD"
    val startTime: String = "",           // "HH:mm"
    val endTime: String = "",             // "HH:mm"
    val format: String = "In-Person",     // "In-Person" | "Virtual" | "Hybrid"
    val location: String = "",
    val virtualPlatform: String = "",
    val virtualLink: String = "",

    // Step 3 — Capacity & Seating
    val capacity: String = "",
    val seatingType: String = "open",     // "open" | "reserved"

    // Step 4 — Pricing & Payment
    val eventType: String = "free",       // "free" | "paid"
    val price: String = "",
    val paymentMethod: String = "",       // "GCash" | "Maya" | "Bank"
    val accountName: String = "",
    val accountNumber: String = "",
    val noRefundPolicy: Boolean = false
)
```

### CreateEventRepository.kt
**Path:** `app/src/main/java/com/hangout/app/repository/CreateEventRepository.kt`

```kotlin
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
```

### ApiService.kt (Updated)
**Path:** `app/src/main/java/com/hangout/app/network/ApiService.kt`

```kotlin
package com.hangout.app.network

import com.hangout.app.data.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    // ── User ──────────────────────────────────────────────────────────────

    @GET("user/profile")
    suspend fun getProfile(): Response<UserProfile>

    @GET("user/stats")
    suspend fun getStats(): Response<UserStats>

    @GET("user/photo")
    suspend fun getPhoto(): Response<PhotoResponse>

    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<MessageResponse>

    @PUT("user/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<MessageResponse>

    @Multipart
    @POST("user/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<MessageResponse>

    @DELETE("user/photo")
    suspend fun deletePhoto(): Response<MessageResponse>

    // ── Events — Discovery ────────────────────────────────────────────────

    @GET("events/discover")
    suspend fun getDiscoverEvents(
        @Query("search") search: String = "",
        @Query("filter") filter: String = ""
    ): Response<List<EventItem>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: Long): Response<EventItem>

    @GET("events/today")
    suspend fun getTodayEvents(): Response<List<EventItem>>

    @GET("events/hosting")
    suspend fun getHostingEvents(): Response<List<EventItem>>

    // ── Events — Creation & Management ────────────────────────────────────

    @POST("events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<CreateEventResponse>

    @PUT("events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body request: CreateEventRequest
    ): Response<CreateEventResponse>

    @Multipart
    @POST("events/{eventId}/cover-image")
    suspend fun uploadEventCoverImage(
        @Path("eventId") eventId: Long,
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): Response<MessageResponse>

    // ── RSVP ──────────────────────────────────────────────────────────────

    @POST("events/{id}/rsvp")
    suspend fun rsvpEvent(@Path("id") id: Long): Response<RsvpResponse>

    @DELETE("events/{id}/rsvp")
    suspend fun cancelRsvp(@Path("id") id: Long): Response<MessageResponse>

    @GET("events/{id}/rsvp/check")
    suspend fun checkRsvpStatus(@Path("id") id: Long): Response<RsvpStatusResponse>

    @GET("events/attending")
    suspend fun getAttendingEvents(): Response<List<EventItem>>

    // ── Host Dashboard ────────────────────────────────────────────────────

    @GET("events/{id}/attendees")
    suspend fun getEventAttendees(@Path("id") id: Long): Response<List<AttendeeItem>>

    @POST("events/{eventId}/rsvp/{rsvpId}/approve")
    suspend fun approvePayment(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: ApprovePaymentRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/reject")
    suspend fun rejectPayment(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: RejectPaymentRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/assign-seat")
    suspend fun assignSeat(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: AssignSeatRequest
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/confirm-attendee")
    suspend fun confirmAttendee(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long
    ): Response<MessageResponse>

    @POST("events/{eventId}/rsvp/{rsvpId}/reject-attendee")
    suspend fun rejectAttendee(
        @Path("eventId") eventId: Long,
        @Path("rsvpId")  rsvpId: Long,
        @Body body: RejectPaymentRequest
    ): Response<MessageResponse>

    @POST("events/{id}/cancel")
    suspend fun cancelEvent(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @Multipart
    @POST("events/{id}/rsvp/payment-proof")
    suspend fun uploadPaymentProof(
        @Path("id") eventId: Long,
        @Part proof: MultipartBody.Part
    ): Response<MessageResponse>

    // ── Notifications ─────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): Response<List<Map<String, Any>>>

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<Map<String, Any>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<Map<String, Any>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, Any>>
}
```

### CreateEventContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/createevent/CreateEventContract.kt`

```kotlin
package com.hangout.app.ui.createevent

import com.hangout.app.data.CreateEventFormState

interface CreateEventContract {

    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun onEventCreated(eventId: Long, isDraft: Boolean)
        fun updateStepIndicator(currentStep: Int, totalSteps: Int)
    }

    interface Presenter {
        fun saveDraft(state: CreateEventFormState, coverImagePath: String?)
        fun publishEvent(state: CreateEventFormState, coverImagePath: String?)
        fun detachView()
    }
}
```

### CreateEventModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/createevent/CreateEventModel.kt`

```kotlin
package com.hangout.app.ui.createevent

import android.content.Context
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.data.CreateEventResponse
import com.hangout.app.repository.CreateEventRepository
import com.hangout.app.repository.Result
import java.io.File

class CreateEventModel(context: Context) {

    private val repo = CreateEventRepository(context)

    suspend fun createEvent(request: CreateEventRequest): Result<CreateEventResponse> =
        repo.createEvent(request)

    suspend fun uploadCoverImage(eventId: Long, imageFile: File): Result<String> =
        repo.uploadCoverImage(eventId, imageFile)
}
```

### CreateEventPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/createevent/CreateEventPresenter.kt`

```kotlin
package com.hangout.app.ui.createevent

import com.hangout.app.data.CreateEventFormState
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class CreateEventPresenter(
    private var view: CreateEventContract.View?,
    private val model: CreateEventModel
) : CreateEventContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Public API ─────────────────────────────────────────────────────────

    override fun saveDraft(state: CreateEventFormState, coverImagePath: String?) {
        submit(state, coverImagePath, isDraft = true)
    }

    override fun publishEvent(state: CreateEventFormState, coverImagePath: String?) {
        val error = validateForPublish(state)
        if (error != null) {
            view?.showError(error)
            return
        }
        submit(state, coverImagePath, isDraft = false)
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }

    // ── Private ────────────────────────────────────────────────────────────

    private fun submit(state: CreateEventFormState, coverImagePath: String?, isDraft: Boolean) {
        val draftError = validateMinimumForDraft(state)
        if (draftError != null) {
            view?.showError(draftError)
            return
        }

        view?.showLoading(true)
        scope.launch {
            val request = buildRequest(state, isDraft)
            when (val result = model.createEvent(request)) {
                is Result.Success -> {
                    val eventId = result.data.id

                    // Phase 2: Upload cover image if provided
                    if (coverImagePath != null) {
                        val file = File(coverImagePath)
                        if (file.exists()) {
                            model.uploadCoverImage(eventId, file)
                            // Non-fatal if upload fails — event already created
                        }
                    }

                    view?.showLoading(false)
                    view?.showSuccess(
                        if (isDraft) "Draft saved!" else "Event published successfully!"
                    )
                    view?.onEventCreated(eventId, isDraft)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /** Minimum required to even save a draft. */
    private fun validateMinimumForDraft(state: CreateEventFormState): String? {
        if (state.title.isBlank()) return "Event title is required"
        return null
    }

    /** Full validation required before publishing. */
    private fun validateForPublish(state: CreateEventFormState): String? {
        if (state.title.isBlank())       return "Event title is required"
        if (state.description.isBlank()) return "Description is required"
        if (state.date.isBlank())        return "Event date is required"
        if (state.startTime.isBlank())   return "Start time is required"

        when (state.format) {
            "In-Person", "Hybrid" ->
                if (state.location.isBlank()) return "Location is required for ${state.format} events"
            "Virtual", "Hybrid" ->
                if (state.virtualLink.isBlank()) return "Virtual meeting link is required"
        }

        val cap = state.capacity.toIntOrNull()
        if (cap == null || cap <= 0) return "Capacity must be a positive number"

        if (state.eventType == "paid") {
            val price = state.price.toIntOrNull()
            if (price == null || price <= 0) return "Price must be a positive number for paid events"
            if (state.paymentMethod.isBlank()) return "Payment method is required for paid events"
            if (state.accountNumber.isBlank()) return "Account number is required for paid events"
        }

        return null
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private fun buildRequest(state: CreateEventFormState, isDraft: Boolean): CreateEventRequest {
        val isPaid = state.eventType == "paid"
        return CreateEventRequest(
            title           = state.title.trim(),
            description     = state.description.trim(),
            date            = state.date,
            startTime       = state.startTime,
            endTime         = state.endTime.ifBlank { null },
            location        = state.location.trim().ifBlank { "TBD" },
            format          = state.format,
            eventType       = state.eventType,
            price           = if (isPaid) (state.price.toIntOrNull() ?: 0) else 0,
            capacity        = state.capacity.toIntOrNull() ?: 0,
            seatingType     = state.seatingType,
            paymentMethod   = if (isPaid) state.paymentMethod.ifBlank { null } else null,
            accountName     = if (isPaid) state.accountName.ifBlank  { null } else null,
            accountNumber   = if (isPaid) state.accountNumber.ifBlank{ null } else null,
            virtualPlatform = if (state.format != "In-Person") state.virtualPlatform.ifBlank { null } else null,
            virtualLink     = if (state.format != "In-Person") state.virtualLink.ifBlank     { null } else null,
            noRefundPolicy  = state.noRefundPolicy,
            isDraft         = isDraft
        )
    }
}
```

### CreateEventActivity.kt
**Path:** `app/src/main/java/com/hangout/app/ui/createevent/CreateEventActivity.kt`

```kotlin
package com.hangout.app.ui.createevent

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.CreateEventFormState
import com.hangout.app.databinding.ActivityCreateEventBinding
import com.hangout.app.utils.getRealPathFromUri
import com.hangout.app.utils.show
import com.hangout.app.utils.hide
import com.hangout.app.utils.toast
import java.util.Calendar

class CreateEventActivity : AppCompatActivity(), CreateEventContract.View {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var presenter: CreateEventContract.Presenter

    private var currentStep = 1
    private val totalSteps  = 4
    private var formState   = CreateEventFormState()
    private var coverImagePath: String? = null

    // ── Image picker ───────────────────────────────────────────────────────

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleImageSelected(uri) }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding   = ActivityCreateEventBinding.inflate(layoutInflater)
        presenter = CreateEventPresenter(this, CreateEventModel(this))
        setContentView(binding.root)

        setupToolbar()
        setupStepNavigation()
        showStep(1)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    // ── Toolbar ────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            if (currentStep > 1) goBack()
            else finish()
        }
    }

    // ── Step navigation ────────────────────────────────────────────────────

    private fun setupStepNavigation() {
        binding.btnNext.setOnClickListener     { handleNext()     }
        binding.btnSaveDraft.setOnClickListener { handleSaveDraft() }
        binding.btnPublish.setOnClickListener   { handlePublish()   }
    }

    private fun showStep(step: Int) {
        currentStep = step
        updateStepIndicator(step, totalSteps)

        binding.layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.layoutStep4.visibility = if (step == 4) View.VISIBLE else View.GONE

        // Button visibility
        binding.btnNext.visibility      = if (step < totalSteps) View.VISIBLE else View.GONE
        binding.btnSaveDraft.visibility = if (step == totalSteps) View.VISIBLE else View.GONE
        binding.btnPublish.visibility   = if (step == totalSteps) View.VISIBLE else View.GONE

        // Populate fields when stepping into a section
        when (step) {
            1 -> setupStep1()
            2 -> setupStep2()
            3 -> setupStep3()
            4 -> setupStep4()
        }

        binding.tvStepTitle.text = stepTitles[step - 1]
    }

    private val stepTitles = listOf(
        "Basic Info",
        "Date, Time & Format",
        "Capacity & Seating",
        "Pricing & Payment"
    )

    private fun handleNext() {
        // Collect current step data first
        collectCurrentStepData()
        val error = validateCurrentStep()
        if (error != null) { toast(error); return }
        showStep(currentStep + 1)
    }

    private fun goBack() {
        collectCurrentStepData()
        showStep(currentStep - 1)
    }

    private fun handleSaveDraft() {
        collectCurrentStepData()
        presenter.saveDraft(formState, coverImagePath)
    }

    private fun handlePublish() {
        collectCurrentStepData()
        presenter.publishEvent(formState, coverImagePath)
    }

    // ── Per-step setup ─────────────────────────────────────────────────────

    private fun setupStep1() {
        binding.etTitle.setText(formState.title)
        binding.etDescription.setText(formState.description)

        // Cover photo preview
        if (coverImagePath != null) {
            Glide.with(this).load(coverImagePath).centerCrop().into(binding.ivCoverPreview)
            binding.ivCoverPreview.show()
            binding.tvCoverPlaceholder.hide()
        }

        binding.layoutCoverPhoto.setOnClickListener { openImagePicker() }
    }

    private fun setupStep2() {

        // Platform dropdown
        val platforms = listOf("Zoom", "Google Meet", "Microsoft Teams", "Webex", "Discord", "Other")
        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platforms)
        binding.etVirtualPlatform.setAdapter(platformAdapter)
        binding.etVirtualPlatform.setOnClickListener { binding.etVirtualPlatform.showDropDown() }
        // Restore saved value
        if (formState.virtualPlatform.isNotBlank()) {
            binding.etVirtualPlatform.setText(formState.virtualPlatform, false)
        }

        binding.etDate.setText(formState.date)
        binding.etStartTime.setText(format24to12(formState.startTime))
        binding.etEndTime.setText(format24to12(formState.endTime))
        binding.etLocation.setText(formState.location)
        binding.etVirtualPlatform.setText(formState.virtualPlatform)
        binding.etVirtualLink.setText(formState.virtualLink)

        // Format selection
        updateFormatSelection(formState.format)
        binding.btnFormatInPerson.setOnClickListener { selectFormat("In-Person") }
        binding.btnFormatVirtual.setOnClickListener  { selectFormat("Virtual")   }
        binding.btnFormatHybrid.setOnClickListener   { selectFormat("Hybrid")    }

        // Date/time pickers
        binding.etDate.setOnClickListener      { showDatePicker()      }
        binding.etStartTime.setOnClickListener { showStartTimePicker() }
        binding.etEndTime.setOnClickListener   { showEndTimePicker()   }
        binding.tilDate.setEndIconOnClickListener      { showDatePicker()      }
        binding.tilStartTime.setEndIconOnClickListener { showStartTimePicker() }
        binding.tilEndTime.setEndIconOnClickListener   { showEndTimePicker()   }
    }

    private fun setupStep3() {
        binding.etCapacity.setText(formState.capacity)
        updateSeatingSelection(formState.seatingType)
        binding.btnSeatingOpen.setOnClickListener     { selectSeating("open")     }
        binding.btnSeatingReserved.setOnClickListener { selectSeating("reserved") }
    }

    private fun setupStep4() {
        binding.etPrice.setText(formState.price)
        binding.etPaymentMethod.setText(formState.paymentMethod)
        binding.etAccountName.setText(formState.accountName)
        binding.etAccountNumber.setText(formState.accountNumber)
        binding.switchNoRefund.isChecked = formState.noRefundPolicy

        updateEventTypeSelection(formState.eventType)
        binding.btnTypeFree.setOnClickListener { selectEventType("free") }
        binding.btnTypePaid.setOnClickListener { selectEventType("paid") }
    }

    // ── Data collection ────────────────────────────────────────────────────

    private fun collectCurrentStepData() {
        formState = when (currentStep) {
            1 -> formState.copy(
                title       = binding.etTitle.text.toString().trim(),
                description = binding.etDescription.text.toString().trim()
            )
            2 -> formState.copy(
                date            = binding.etDate.text.toString(),
                startTime       = binding.etStartTime.text.toString(),
                endTime         = binding.etEndTime.text.toString(),
                location        = binding.etLocation.text.toString().trim(),
                virtualPlatform = binding.etVirtualPlatform.text.toString().trim(),
                virtualLink     = binding.etVirtualLink.text.toString().trim()
            )
            3 -> formState.copy(
                capacity = binding.etCapacity.text.toString()
            )
            4 -> formState.copy(
                price         = binding.etPrice.text.toString(),
                paymentMethod = binding.etPaymentMethod.text.toString().trim(),
                accountName   = binding.etAccountName.text.toString().trim(),
                accountNumber = binding.etAccountNumber.text.toString().trim(),
                noRefundPolicy = binding.switchNoRefund.isChecked
            )
            else -> formState
        }
    }

    // ── Per-step validation ────────────────────────────────────────────────

    private fun validateCurrentStep(): String? = when (currentStep) {
        1 -> {
            val title = binding.etTitle.text.toString().trim()
            if (title.isBlank()) "Event title is required" else null
        }
        2 -> {
            val date  = binding.etDate.text.toString()
            val start = binding.etStartTime.text.toString()
            when {
                date.isBlank()  -> "Event date is required"
                start.isBlank() -> "Start time is required"
                else -> null
            }
        }
        3 -> {
            val cap = binding.etCapacity.text.toString().toIntOrNull()
            if (cap == null || cap <= 0) "Enter a valid capacity (e.g. 50)" else null
        }
        else -> null
    }

    // ── Format & Type helpers ──────────────────────────────────────────────

    private fun selectFormat(format: String) {
        formState = formState.copy(format = format)
        updateFormatSelection(format)
    }

    private fun updateFormatSelection(format: String) {
        val active   = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white    = ContextCompat.getColor(this, R.color.white)
        val muted    = ContextCompat.getColor(this, R.color.text_muted)

        fun style(btn: Button, selected: Boolean) {
            btn.setBackgroundColor(if (selected) active else inactive)
            btn.setTextColor(if (selected) white else muted)
        }

        style(binding.btnFormatInPerson, format == "In-Person")
        style(binding.btnFormatVirtual,  format == "Virtual")
        style(binding.btnFormatHybrid,   format == "Hybrid")

        // Show/hide location and virtual fields based on format
        val showLocation = format != "Virtual"
        val showVirtual  = format != "In-Person"
        binding.tilLocation.visibility        = if (showLocation) View.VISIBLE else View.GONE
        binding.layoutVirtualFields.visibility = if (showVirtual) View.VISIBLE else View.GONE
    }

    private fun selectSeating(type: String) {
        formState = formState.copy(seatingType = type)
        updateSeatingSelection(type)
    }

    private fun updateSeatingSelection(type: String) {
        val active   = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white    = ContextCompat.getColor(this, R.color.white)
        val muted    = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnSeatingOpen.setBackgroundColor(if (type == "open") active else inactive)
        binding.btnSeatingOpen.setTextColor(if (type == "open") white else muted)
        binding.btnSeatingReserved.setBackgroundColor(if (type == "reserved") active else inactive)
        binding.btnSeatingReserved.setTextColor(if (type == "reserved") white else muted)
    }

    private fun selectEventType(type: String) {
        formState = formState.copy(eventType = type)
        updateEventTypeSelection(type)
    }

    private fun updateEventTypeSelection(type: String) {
        val active   = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white    = ContextCompat.getColor(this, R.color.white)
        val muted    = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnTypeFree.setBackgroundColor(if (type == "free") active else inactive)
        binding.btnTypeFree.setTextColor(if (type == "free") white else muted)
        binding.btnTypePaid.setBackgroundColor(if (type == "paid") active else inactive)
        binding.btnTypePaid.setTextColor(if (type == "paid") white else muted)

        binding.layoutPaidFields.visibility = if (type == "paid") View.VISIBLE else View.GONE
    }

    // ── Date / Time pickers ────────────────────────────────────────────────

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this,
            { _, year, month, day ->
                val mm = String.format("%02d", month + 1)
                val dd = String.format("%02d", day)
                val formatted = "$year-$mm-$dd"
                formState = formState.copy(date = formatted)
                binding.etDate.setText(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000 // today or future
        }.show()
    }

    private fun showStartTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val formatted24 = String.format("%02d:%02d", hour, minute)
            formState = formState.copy(startTime = formatted24)
            binding.etStartTime.setText(to12Hr(hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun showEndTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val formatted24 = String.format("%02d:%02d", hour, minute)
            formState = formState.copy(endTime = formatted24)
            binding.etEndTime.setText(to12Hr(hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    // ── Image picker ───────────────────────────────────────────────────────

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        coverImagePath = getRealPathFromUri(this, uri) ?: uri.toString()
        Glide.with(this).load(uri).centerCrop().into(binding.ivCoverPreview)
        binding.ivCoverPreview.show()
        binding.tvCoverPlaceholder.hide()
    }

    // ── CreateEventContract.View ───────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.visibility  = if (show) View.VISIBLE else View.GONE
        binding.btnPublish.isEnabled    = !show
        binding.btnSaveDraft.isEnabled  = !show
        binding.btnNext.isEnabled       = !show
    }

    override fun showError(message: String) {
        toast(message)
    }

    override fun showSuccess(message: String) {
        toast(message)
    }

    override fun onEventCreated(eventId: Long, isDraft: Boolean) {
        val resultIntent = Intent().apply {
            putExtra("eventId", eventId)
            putExtra("isDraft", isDraft)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun updateStepIndicator(currentStep: Int, totalSteps: Int) {
        binding.tvStepCounter.text = "Step $currentStep of $totalSteps"

        // Update step dot indicators
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        val active   = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        dots.forEachIndexed { index, view ->
            view.setBackgroundColor(if (index < currentStep) active else inactive)
        }
    }

    private fun to12Hr(hour: Int, minute: Int): String {
        val ampm  = if (hour >= 12) "PM" else "AM"
        val h12   = when {
            hour == 0  -> 12
            hour > 12  -> hour - 12
            else       -> hour
        }
        return String.format("%d:%02d %s", h12, minute, ampm)
    }

    private fun format24to12(time24: String): String {
        if (time24.isBlank()) return ""
        return try {
            val parts = time24.split(":")
            to12Hr(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) { time24 }
    }
}
```

### FileUtils.kt
**Path:** `app/src/main/java/com/hangout/app/utils/FileUtils.kt`

```kotlin
package com.hangout.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Resolves a content:// URI to an absolute file path the OS can read directly.
 *
 * Strategy:
 *  1. Try the MediaStore DATA column (fast path — works for gallery picks on most devices).
 *  2. If that returns null (scoped storage, Downloads, Drive, etc.) copy the stream
 *     to a temp file in the app's cache dir and return that path instead.
 *
 * The returned path is always readable by File(path) and can be passed directly
 * to OkHttp's RequestBody.asRequestBody().
 */
fun getRealPathFromUri(context: Context, uri: Uri): String? {
    // ── 1. Fast path: MediaStore DATA column ──────────────────────────────
    if (uri.scheme == "content") {
        val dataPath = queryMediaStoreData(context, uri)
        if (!dataPath.isNullOrBlank()) return dataPath
    }

    // ── 2. file:// URI — already a real path ──────────────────────────────
    if (uri.scheme == "file") return uri.path

    // ── 3. Fallback: copy stream to cache file ────────────────────────────
    return copyUriToCache(context, uri)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun queryMediaStoreData(context: Context, uri: Uri): String? {
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.let {
            val col = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (it.moveToFirst()) it.getString(col) else null
        }
    } catch (_: Exception) {
        null
    } finally {
        cursor?.close()
    }
}

/**
 * Copies the content behind [uri] into a uniquely-named temp file under
 * [Context.cacheDir]/hangout_picks/ and returns its absolute path.
 * Returns null if the stream cannot be opened or the copy fails.
 */
fun copyUriToCache(context: Context, uri: Uri): String? {
    return try {
        val pickDir = File(context.cacheDir, "hangout_picks").apply { mkdirs() }
        val fileName = resolveFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg"
        val dest = File(pickDir, fileName)

        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        FileOutputStream(dest).use { out ->
            inputStream.use { it.copyTo(out) }
        }
        dest.absolutePath
    } catch (_: Exception) {
        null
    }
}

/** Reads the display name from the content resolver (used for the cache file name). */
private fun resolveFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = it.getString(idx)
        }
    }
    return name?.takeIf { it.isNotBlank() }
}

/**
 * Clears all files previously copied to the hangout_picks cache directory.
 * Call this from onDestroy or after an upload to avoid accumulating stale images.
 */
fun clearPickCache(context: Context) {
    try {
        File(context.cacheDir, "hangout_picks").deleteRecursively()
    } catch (_: Exception) { /* non-fatal */ }
}
```

### activity_create_event.xml
**Path:** `app/src/main/res/layout/activity_create_event.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  activity_create_event.xml
  4-step event creation wizard.

  Binding IDs used by CreateEventActivity:
    Header  : btnBack, tvStepTitle, tvStepCounter, dot1-dot4
    Nav     : btnNext, btnSaveDraft, btnPublish, progressBar
    Step 1  : layoutStep1, layoutCoverPhoto, ivCoverPreview, tvCoverPlaceholder,
              etTitle, etDescription
    Step 2  : layoutStep2, btnFormatInPerson, btnFormatVirtual, btnFormatHybrid,
              tilDate, etDate, tilStartTime, etStartTime, tilEndTime, etEndTime,
              tilLocation, etLocation, layoutVirtualFields, etVirtualPlatform, etVirtualLink
    Step 3  : layoutStep3, etCapacity, btnSeatingOpen, btnSeatingReserved
    Step 4  : layoutStep4, btnTypeFree, btnTypePaid, etPrice,
              layoutPaidFields, etPaymentMethod, etAccountName, etAccountNumber,
              switchNoRefund
-->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_dark">

    <!-- ── Sticky Header ──────────────────────────────────────────────── -->
    <LinearLayout
        android:id="@+id/headerBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="@color/bg_dark"
        android:elevation="4dp"
        android:paddingTop="48dp"
        android:paddingBottom="12dp"
        android:paddingHorizontal="20dp"
        app:layout_behavior="com.google.android.material.appbar.AppBarLayout$ScrollingViewBehavior">

        <!-- Row: back + title + counter -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginBottom="14dp">

            <ImageButton
                android:id="@+id/btnBack"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:background="@drawable/btn_icon_bg"
                android:src="@drawable/ic_back"
                android:contentDescription="Back"
                app:tint="@color/text_primary"
                android:layout_marginEnd="12dp" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvStepTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Basic Info"
                    android:textColor="@color/text_primary"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:fontFamily="@font/syne_bold" />

                <TextView
                    android:id="@+id/tvStepCounter"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Step 1 of 4"
                    android:textColor="@color/text_muted"
                    android:textSize="12sp"
                    android:layout_marginTop="2dp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Step dot indicators -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_horizontal"
            android:weightSum="4">

            <View
                android:id="@+id/dot1"
                android:layout_width="0dp"
                android:layout_height="4dp"
                android:layout_weight="1"
                android:layout_marginHorizontal="3dp"
                android:background="@color/purple_main"
                android:backgroundTint="@color/purple_main"
                app:backgroundTint="@null" />

            <View
                android:id="@+id/dot2"
                android:layout_width="0dp"
                android:layout_height="4dp"
                android:layout_weight="1"
                android:layout_marginHorizontal="3dp"
                android:background="@color/bg_card" />

            <View
                android:id="@+id/dot3"
                android:layout_width="0dp"
                android:layout_height="4dp"
                android:layout_weight="1"
                android:layout_marginHorizontal="3dp"
                android:background="@color/bg_card" />

            <View
                android:id="@+id/dot4"
                android:layout_width="0dp"
                android:layout_height="4dp"
                android:layout_weight="1"
                android:layout_marginHorizontal="3dp"
                android:background="@color/bg_card" />
        </LinearLayout>
    </LinearLayout>

    <!-- ── Scrollable content + bottom nav ───────────────────────────── -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:layout_marginTop="134dp">

        <!-- Scrollable step panels -->
        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:fillViewport="true"
            android:scrollbarStyle="outsideOverlay">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:paddingHorizontal="20dp"
                android:paddingTop="8dp"
                android:paddingBottom="24dp">

                <!-- ══════════════ STEP 1 – Basic Info ══════════════ -->
                <LinearLayout
                    android:id="@+id/layoutStep1"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="visible">

                    <!-- Cover photo picker -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Cover Photo"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <FrameLayout
                        android:id="@+id/layoutCoverPhoto"
                        android:layout_width="match_parent"
                        android:layout_height="180dp"
                        android:background="@drawable/cover_photo_bg"
                        android:clickable="true"
                        android:focusable="true"
                        android:layout_marginBottom="20dp">

                        <ImageView
                            android:id="@+id/ivCoverPreview"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:scaleType="centerCrop"
                            android:visibility="gone" />

                        <LinearLayout
                            android:id="@+id/tvCoverPlaceholder"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_gravity="center"
                            android:orientation="vertical"
                            android:gravity="center">

                            <ImageView
                                android:layout_width="32dp"
                                android:layout_height="32dp"
                                android:src="@drawable/ic_upload"
                                app:tint="@color/text_muted"
                                android:layout_marginBottom="8dp" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Tap to upload cover photo"
                                android:textColor="@color/text_muted"
                                android:textSize="13sp" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="JPG or PNG, max 5 MB"
                                android:textColor="@color/text_muted"
                                android:textSize="11sp"
                                android:alpha="0.6"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>
                    </FrameLayout>

                    <!-- Event title -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Event Title *"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                        android:layout_marginBottom="16dp"
                        app:boxStrokeColor="@color/input_stroke_selector"
                        app:boxBackgroundColor="@color/bg_card"
                        app:hintTextColor="@color/text_muted">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etTitle"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="e.g. Sunday Basketball Meetup"
                            android:textColor="@color/text_primary"
                            android:textColorHint="@color/text_muted"
                            android:textSize="15sp"
                            android:inputType="textCapSentences"
                            android:maxLines="1"
                            android:imeOptions="actionNext" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Description -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Description"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                        app:boxStrokeColor="@color/input_stroke_selector"
                        app:boxBackgroundColor="@color/bg_card"
                        app:hintTextColor="@color/text_muted">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etDescription"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Tell people what this HangOut is about…"
                            android:textColor="@color/text_primary"
                            android:textColorHint="@color/text_muted"
                            android:textSize="15sp"
                            android:inputType="textCapSentences|textMultiLine"
                            android:minLines="4"
                            android:maxLines="8"
                            android:gravity="top"
                            android:paddingTop="12dp" />
                    </com.google.android.material.textfield.TextInputLayout>
                </LinearLayout>

                <!-- ══════════════ STEP 2 – Date / Time / Format ══════════════ -->
                <LinearLayout
                    android:id="@+id/layoutStep2"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="gone">

                    <!-- Event Format -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Event Format"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="10dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="20dp">

                        <Button
                            android:id="@+id/btnFormatInPerson"
                            android:layout_width="0dp"
                            android:layout_height="40dp"
                            android:layout_weight="1"
                            android:text="In-Person"
                            android:textSize="12sp"
                            android:backgroundTint="@color/purple_main"
                            android:textColor="@color/white"
                            android:layout_marginEnd="6dp"
                            style="@style/Widget.MaterialComponents.Button" />

                        <Button
                            android:id="@+id/btnFormatVirtual"
                            android:layout_width="0dp"
                            android:layout_height="40dp"
                            android:layout_weight="1"
                            android:text="Virtual"
                            android:textSize="12sp"
                            android:backgroundTint="@color/bg_card"
                            android:textColor="@color/text_muted"
                            android:layout_marginEnd="6dp"
                            style="@style/Widget.MaterialComponents.Button" />

                        <Button
                            android:id="@+id/btnFormatHybrid"
                            android:layout_width="0dp"
                            android:layout_height="40dp"
                            android:layout_weight="1"
                            android:text="Hybrid"
                            android:textSize="12sp"
                            android:backgroundTint="@color/bg_card"
                            android:textColor="@color/text_muted"
                            style="@style/Widget.MaterialComponents.Button" />
                    </LinearLayout>

                    <!-- Date -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Date *"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/tilDate"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                        app:endIconMode="custom"
                        app:endIconDrawable="@drawable/ic_calendar"
                        app:endIconTint="@color/text_muted"
                        app:boxStrokeColor="@color/input_stroke_selector"
                        app:boxBackgroundColor="@color/bg_card"
                        android:layout_marginBottom="16dp">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etDate"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="YYYY-MM-DD"
                            android:textColor="@color/text_primary"
                            android:textColorHint="@color/text_muted"
                            android:textSize="15sp"
                            android:focusable="false"
                            android:clickable="true"
                            android:cursorVisible="false" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Start / End time row -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="16dp">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:layout_marginEnd="8dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Start Time *"
                                android:textColor="@color/text_primary"
                                android:textSize="14sp"
                                android:textStyle="bold"
                                android:layout_marginBottom="8dp" />

                            <com.google.android.material.textfield.TextInputLayout
                                android:id="@+id/tilStartTime"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                                app:endIconMode="custom"
                                app:endIconDrawable="@drawable/ic_clock"
                                app:endIconTint="@color/text_muted"
                                app:boxStrokeColor="@color/input_stroke_selector"
                                app:boxBackgroundColor="@color/bg_card">

                                <com.google.android.material.textfield.TextInputEditText
                                    android:id="@+id/etStartTime"
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:hint="HH:MM"
                                    android:textColor="@color/text_primary"
                                    android:textColorHint="@color/text_muted"
                                    android:textSize="15sp"
                                    android:focusable="false"
                                    android:clickable="true"
                                    android:cursorVisible="false" />
                            </com.google.android.material.textfield.TextInputLayout>
                        </LinearLayout>

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="End Time"
                                android:textColor="@color/text_primary"
                                android:textSize="14sp"
                                android:textStyle="bold"
                                android:layout_marginBottom="8dp" />

                            <com.google.android.material.textfield.TextInputLayout
                                android:id="@+id/tilEndTime"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                                app:endIconMode="custom"
                                app:endIconDrawable="@drawable/ic_clock"
                                app:endIconTint="@color/text_muted"
                                app:boxStrokeColor="@color/input_stroke_selector"
                                app:boxBackgroundColor="@color/bg_card">

                                <com.google.android.material.textfield.TextInputEditText
                                    android:id="@+id/etEndTime"
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:hint="HH:MM"
                                    android:textColor="@color/text_primary"
                                    android:textColorHint="@color/text_muted"
                                    android:textSize="15sp"
                                    android:focusable="false"
                                    android:clickable="true"
                                    android:cursorVisible="false" />
                            </com.google.android.material.textfield.TextInputLayout>
                        </LinearLayout>
                    </LinearLayout>

                    <!-- Location (hidden for Virtual) -->
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/tilLocation"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                        android:hint="Location *"
                        app:boxStrokeColor="@color/input_stroke_selector"
                        app:boxBackgroundColor="@color/bg_card"
                        app:hintTextColor="@color/text_muted"
                        android:layout_marginBottom="16dp">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etLocation"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="e.g. Ayala Center Cebu, Lahug"
                            android:textColor="@color/text_primary"
                            android:textColorHint="@color/text_muted"
                            android:textSize="15sp"
                            android:inputType="textCapWords"
                            android:imeOptions="actionNext" />
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Virtual fields (hidden for In-Person) -->
                    <LinearLayout
                        android:id="@+id/layoutVirtualFields"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:visibility="gone">

                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Platform (e.g. Zoom, Google Meet)"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="12dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etVirtualPlatform"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="text"
                                android:imeOptions="actionNext" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>

                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Meeting Link *"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="16dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etVirtualLink"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="textUri"
                                android:imeOptions="actionDone" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>
                    </LinearLayout>
                </LinearLayout>

                <!-- ══════════════ STEP 3 – Capacity & Seating ══════════════ -->
                <LinearLayout
                    android:id="@+id/layoutStep3"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="gone">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Max Capacity *"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="8dp" />

                    <com.google.android.material.textfield.TextInputLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                        android:hint="Number of slots"
                        app:boxStrokeColor="@color/input_stroke_selector"
                        app:boxBackgroundColor="@color/bg_card"
                        app:hintTextColor="@color/text_muted"
                        android:layout_marginBottom="28dp">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/etCapacity"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textColorHint="@color/text_muted"
                            android:textSize="15sp"
                            android:inputType="number"
                            android:imeOptions="actionDone" />
                    </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>

                    <!-- Seating type -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Seating Type"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="10dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="16dp">

                        <Button
                            android:id="@+id/btnSeatingOpen"
                            android:layout_width="0dp"
                            android:layout_height="48dp"
                            android:layout_weight="1"
                            android:text="Open Seating"
                            android:textSize="13sp"
                            android:backgroundTint="@color/purple_main"
                            android:textColor="@color/white"
                            android:layout_marginEnd="10dp"
                            style="@style/Widget.MaterialComponents.Button" />

                        <Button
                            android:id="@+id/btnSeatingReserved"
                            android:layout_width="0dp"
                            android:layout_height="48dp"
                            android:layout_weight="1"
                            android:text="Reserved Seats"
                            android:textSize="13sp"
                            android:backgroundTint="@color/bg_card"
                            android:textColor="@color/text_muted"
                            style="@style/Widget.MaterialComponents.Button" />
                    </LinearLayout>

                    <!-- Info card about seating -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:background="@drawable/card_info_bg"
                        android:padding="14dp"
                        android:gravity="center_vertical">

                        <ImageView
                            android:layout_width="18dp"
                            android:layout_height="18dp"
                            android:src="@drawable/ic_info"
                            app:tint="@color/purple_light"
                            android:layout_marginEnd="10dp" />

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="With Reserved Seating, attendees will be assigned a numbered seat after payment approval."
                            android:textColor="@color/text_muted"
                            android:textSize="12sp"
                            android:lineSpacingExtra="2dp" />
                    </LinearLayout>
                </LinearLayout>

                <!-- ══════════════ STEP 4 – Pricing & Payment ══════════════ -->
                <LinearLayout
                    android:id="@+id/layoutStep4"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:visibility="gone">

                    <!-- Event type toggle -->
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Event Type"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="10dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="24dp">

                        <Button
                            android:id="@+id/btnTypeFree"
                            android:layout_width="0dp"
                            android:layout_height="48dp"
                            android:layout_weight="1"
                            android:text="Free"
                            android:textSize="14sp"
                            android:backgroundTint="@color/purple_main"
                            android:textColor="@color/white"
                            android:layout_marginEnd="10dp"
                            style="@style/Widget.MaterialComponents.Button" />

                        <Button
                            android:id="@+id/btnTypePaid"
                            android:layout_width="0dp"
                            android:layout_height="48dp"
                            android:layout_weight="1"
                            android:text="Paid"
                            android:textSize="14sp"
                            android:backgroundTint="@color/bg_card"
                            android:textColor="@color/text_muted"
                            style="@style/Widget.MaterialComponents.Button" />
                    </LinearLayout>

                    <!-- Paid-only fields -->
                    <LinearLayout
                        android:id="@+id/layoutPaidFields"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:visibility="gone">

                        <!-- Price -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Ticket Price (₱)"
                            app:prefixText="₱ "
                            app:prefixTextColor="@color/text_muted"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="14dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etPrice"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="numberDecimal"
                                android:imeOptions="actionNext" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>

                        <!-- Payment method -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Payment Method (e.g. GCash, Maya)"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="14dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etPaymentMethod"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="text"
                                android:imeOptions="actionNext" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>

                        <!-- Account name -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Account Name"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="14dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etAccountName"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="textPersonName|textCapWords"
                                android:imeOptions="actionNext" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>

                        <!-- Account number -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
                            android:hint="Account Number / Mobile Number"
                            app:boxStrokeColor="@color/input_stroke_selector"
                            app:boxBackgroundColor="@color/bg_card"
                            app:hintTextColor="@color/text_muted"
                            android:layout_marginBottom="16dp">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/etAccountNumber"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:textColor="@color/text_primary"
                                android:textColorHint="@color/text_muted"
                                android:textSize="15sp"
                                android:inputType="phone"
                                android:imeOptions="actionDone" />
                        </com.google.android.material.textfield.TextInputEditText>
                    </com.google.android.material.textfield.TextInputLayout>
                    </LinearLayout>

                    <!-- No-refund toggle (always visible on step 4) -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:background="@drawable/card_bg"
                        android:padding="16dp"
                        android:layout_marginTop="8dp">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical"
                            android:layout_marginEnd="12dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="No Refund Policy"
                                android:textColor="@color/text_primary"
                                android:textSize="14sp"
                                android:textStyle="bold" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Attendees will not be able to request refunds"
                                android:textColor="@color/text_muted"
                                android:textSize="12sp"
                                android:layout_marginTop="2dp" />
                        </LinearLayout>

                        <com.google.android.material.switchmaterial.SwitchMaterial
                            android:id="@+id/switchNoRefund"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:checked="false"
                            app:thumbTint="@color/switch_thumb_selector"
                            app:trackTint="@color/switch_track_selector" />
                    </LinearLayout>
                </LinearLayout>

            </LinearLayout>
        </ScrollView>

        <!-- ── Sticky Bottom Navigation ──────────────────────────────── -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@color/bg_dark"
            android:elevation="8dp">

            <!-- Divider -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@color/divider" />

            <!-- Loading bar (hidden by default) -->
            <ProgressBar
                android:id="@+id/progressBar"
                style="@style/Widget.AppCompat.ProgressBar.Horizontal"
                android:layout_width="match_parent"
                android:layout_height="3dp"
                android:indeterminate="true"
                android:visibility="gone"
                android:progressTint="@color/purple_main"
                android:indeterminateTint="@color/purple_main" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:padding="16dp">

                <!-- Next (steps 1-3) -->
                <Button
                    android:id="@+id/btnNext"
                    android:layout_width="match_parent"
                    android:layout_height="52dp"
                    android:text="Continue →"
                    android:textSize="15sp"
                    android:textStyle="bold"
                    android:backgroundTint="@color/purple_main"
                    android:textColor="@color/white"
                    style="@style/Widget.MaterialComponents.Button" />

                <!-- Save Draft (step 4 only, hidden initially) -->
                <Button
                    android:id="@+id/btnSaveDraft"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Save Draft"
                    android:textSize="14sp"
                    android:backgroundTint="@color/bg_card"
                    android:textColor="@color/text_primary"
                    android:layout_marginEnd="10dp"
                    android:visibility="gone"
                    style="@style/Widget.MaterialComponents.Button" />

                <!-- Publish (step 4 only, hidden initially) -->
                <Button
                    android:id="@+id/btnPublish"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Publish 🎉"
                    android:textSize="14sp"
                    android:backgroundTint="@color/purple_main"
                    android:textColor="@color/white"
                    android:visibility="gone"
                    style="@style/Widget.MaterialComponents.Button" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

---

## HOST DASHBOARD SCREEN

### HostDashboardContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/hostdashboard/HostDashboardContract.kt`

```kotlin
package com.hangout.app.ui.hostdashboard

import com.hangout.app.data.AttendeeItem
import com.hangout.app.data.EventItem

interface HostDashboardContract {

    interface View {
        fun showEvent(event: EventItem)
        fun showAttendees(attendees: List<AttendeeItem>)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onActionSuccess(rsvpId: Long, newStatus: String)
        fun onEventCancelled()
        fun onEventDeleted()
    }

    interface Presenter {
        fun loadAttendees(eventId: Long)
        fun approvePayment(eventId: Long, rsvpId: Long, seatNumber: String?)
        fun rejectPayment(eventId: Long, rsvpId: Long, reason: String)
        fun assignSeat(eventId: Long, rsvpId: Long, seatNumber: String)
        fun confirmAttendee(eventId: Long, rsvpId: Long)
        fun rejectAttendee(eventId: Long, rsvpId: Long, reason: String)
        fun cancelEvent(eventId: Long, reason: String)
        fun deleteEvent(eventId: Long)
        fun detachView()
    }
}
```

### HostDashboardModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/hostdashboard/HostDashboardModel.kt`

```kotlin
package com.hangout.app.ui.hostdashboard

import android.content.Context
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class HostDashboardModel(private val context: Context) {

    private val api = RetrofitClient.getApiService(context)


    suspend fun getAttendees(eventId: Long): Result<List<AttendeeItem>> {
        val key = AppCache.Keys.eventAttendees(eventId)
        AppCache.get(context, key)?.let { json ->
            val type = object : com.google.gson.reflect.TypeToken<List<AttendeeItem>>() {}.type
            return Result.Success(com.google.gson.Gson().fromJson(json, type))
        }
        return try {
            val response = api.getEventAttendees(eventId)
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppCache.put(context, key, com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                Result.Success(body)
            } else Result.Error("Failed to load attendees (${response.code()})")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun approvePayment(
        eventId: Long, rsvpId: Long, seatNumber: String?
    ): Result<MessageResponse> {
        return try {
            val body = ApprovePaymentRequest(seatNumber = seatNumber)
            val response = api.approvePayment(eventId, rsvpId, body)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                Result.Success(response.body() ?: MessageResponse("Approved"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun rejectPayment(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> {
        return try {
            val response = api.rejectPayment(eventId, rsvpId, RejectPaymentRequest(reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Rejected"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun assignSeat(
        eventId: Long, rsvpId: Long, seatNumber: String
    ): Result<MessageResponse> {
        return try {
            val response = api.assignSeat(eventId, rsvpId, AssignSeatRequest(seatNumber))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Seat assigned"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun confirmAttendee(eventId: Long, rsvpId: Long): Result<MessageResponse> {
        return try {
            val response = api.confirmAttendee(eventId, rsvpId)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                Result.Success(response.body() ?: MessageResponse("Confirmed"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun rejectAttendee(
        eventId: Long, rsvpId: Long, reason: String
    ): Result<MessageResponse> {
        return try {
            val response = api.rejectAttendee(eventId, rsvpId, RejectPaymentRequest(reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.eventAttendees(eventId))
                Result.Success(response.body() ?: MessageResponse("Rejected"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun cancelEvent(eventId: Long, reason: String): Result<MessageResponse> {
        return try {
            val response = api.cancelEvent(eventId, mapOf("reason" to reason))
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.ATTENDING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.TODAY_EVENTS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Event cancelled"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deleteEvent(eventId: Long): Result<MessageResponse> {
        return try {
            val response = api.deleteEvent(eventId)
            if (response.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                AppCache.bust(context, AppCache.Keys.STATS)
                AppCache.bust(context, AppCache.Keys.DISCOVER_EVENTS)
                Result.Success(response.body() ?: MessageResponse("Event deleted"))
            } else
                Result.Error(parseError(response.errorBody()?.string()))
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    private fun parseError(body: String?): String {
        if (body == null) return "Server error"
        return try {
            org.json.JSONObject(body).optString("message", "Server error")
        } catch (_: Exception) { "Server error" }
    }
}
```

### HostDashboardPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/hostdashboard/HostDashboardPresenter.kt`

```kotlin
package com.hangout.app.ui.hostdashboard

import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class HostDashboardPresenter(
    private var view: HostDashboardContract.View?,
    private val model: HostDashboardModel
) : HostDashboardContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAttendees(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getAttendees(eventId)) {
                is Result.Success -> view?.showAttendees(r.data)
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun approvePayment(eventId: Long, rsvpId: Long, seatNumber: String?) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.approvePayment(eventId, rsvpId, seatNumber)) {
                is Result.Success -> {
                    view?.showMessage("Payment approved!")
                    view?.onActionSuccess(rsvpId, "confirmed")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun rejectPayment(eventId: Long, rsvpId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.rejectPayment(eventId, rsvpId, reason)) {
                is Result.Success -> {
                    view?.showMessage("Payment rejected.")
                    view?.onActionSuccess(rsvpId, "rejected")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun assignSeat(eventId: Long, rsvpId: Long, seatNumber: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.assignSeat(eventId, rsvpId, seatNumber)) {
                is Result.Success -> {
                    view?.showMessage("Seat $seatNumber assigned.")
                    view?.onActionSuccess(rsvpId, "seat_assigned")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun confirmAttendee(eventId: Long, rsvpId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.confirmAttendee(eventId, rsvpId)) {
                is Result.Success -> {
                    view?.showMessage("Attendee confirmed.")
                    view?.onActionSuccess(rsvpId, "attendee_confirmed")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun rejectAttendee(eventId: Long, rsvpId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.rejectAttendee(eventId, rsvpId, reason)) {
                is Result.Success -> {
                    view?.showMessage("Attendee rejected.")
                    view?.onActionSuccess(rsvpId, "attendee_rejected")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun cancelEvent(eventId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.cancelEvent(eventId, reason)) {
                is Result.Success -> view?.onEventCancelled()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun deleteEvent(eventId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.deleteEvent(eventId)) {
                is Result.Success -> view?.onEventDeleted()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### HostDashboardFragment.kt
**Path:** `app/src/main/java/com/hangout/app/ui/hostdashboard/HostDashboardFragment.kt`

```kotlin
package com.hangout.app.ui.hostdashboard

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.AttendeeItem
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentHostDashboardBinding
import com.hangout.app.databinding.ItemAttendeeRowBinding
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class HostDashboardFragment : Fragment(), HostDashboardContract.View {

    private var _binding: FragmentHostDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HostDashboardContract.Presenter

    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            HostDashboardFragment().apply { onBackCallback = onBack }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = HostDashboardPresenter(this, HostDashboardModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: return

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { e ->
            showEvent(e)
            e.id?.let { presenter.loadAttendees(it) }
        }

        binding.btnCancelEvent.setOnClickListener { promptCancelEvent() }
        binding.btnDeleteEvent.setOnClickListener { promptDeleteEvent() }
        binding.btnRefresh.setOnClickListener {
            event?.id?.let { presenter.loadAttendees(it) }
        }
    }

    override fun showEvent(event: EventItem) {
        this.event = event
        binding.tvEventTitle.text = event.title ?: "Untitled"
        binding.tvEventMeta.text  = "${event.date ?: "—"} · ${event.startTime ?: event.time ?: "—"}"
        binding.tvEventLocation.text = event.location ?: "Virtual / TBD"

        val total   = event.capacity ?: 0
        val current = event.attendeeCount ?: 0
        binding.tvSlots.text = "$current / $total attendees"
        if (total > 0) {
            binding.progressSlots.max      = total
            binding.progressSlots.progress = current
        }

        val isCancelled = event.eventStatus == "cancelled"
        binding.btnCancelEvent.showIf(!isCancelled)
        binding.tvCancelledBanner.showIf(isCancelled)
    }

    override fun showAttendees(attendees: List<AttendeeItem>) {
        binding.layoutAttendeesContainer.removeAllViews()
        if (attendees.isEmpty()) {
            binding.layoutAttendeesContainer.addView(emptyText("No RSVPs yet."))
            return
        }

        val pending   = attendees.count { it.paymentStatus == "pending" }
        val confirmed = attendees.count { it.paymentStatus == "confirmed" || it.status == "confirmed" }
        val cancelled = attendees.count { it.status == "cancelled" }
        binding.tvSummary.text = "Pending: $pending · Confirmed: $confirmed · Cancelled: $cancelled"
        binding.tvSummary.show()

        attendees.forEach { attendee ->
            val row = ItemAttendeeRowBinding.inflate(layoutInflater, binding.layoutAttendeesContainer, false)
            bindAttendeeRow(row, attendee)
            binding.layoutAttendeesContainer.addView(row.root)
        }
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) = toast(message)

    override fun onActionSuccess(rsvpId: Long, newStatus: String) {
        event?.id?.let { presenter.loadAttendees(it) }
    }

    override fun onEventCancelled() {
        toast("Event cancelled.")
        binding.tvCancelledBanner.show()
        binding.btnCancelEvent.hide()
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    override fun onEventDeleted() {
        toast("Event deleted.")
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    private fun bindAttendeeRow(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val ctx = requireContext()
        row.tvAttendeeName.text = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim().ifBlank { attendee.email ?: "Unknown" }
        row.tvAttendeeEmail.text = attendee.email ?: ""

        if (!attendee.photo.isNullOrBlank()) {
            Glide.with(this).load(attendee.photo).circleCrop().into(row.ivAvatar)
        }

        val (label, color) = resolveStatus(attendee)
        row.tvStatusBadge.text = label
        row.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, color))

        if (!attendee.seatNumber.isNullOrBlank()) {
            row.tvSeatNumber.text = "Seat ${attendee.seatNumber}"
            row.tvSeatNumber.show()
        } else {
            row.tvSeatNumber.hide()
        }

        val hasProof = !attendee.paymentProofUrl.isNullOrBlank()
        row.btnViewProof.showIf(hasProof)
        row.btnViewProof.setOnClickListener { showPaymentProofDialog(attendee) }

        setupActionButtons(row, attendee)
    }

    private fun resolveStatus(a: AttendeeItem): Pair<String, Int> = when {
        a.status == "cancelled"         -> "Cancelled"    to R.color.text_muted
        a.attendeeStatus == "rejected"  -> "Rejected"     to R.color.red_accent
        a.attendeeStatus == "attended"  -> "Attended ✓"   to R.color.success_green
        a.paymentStatus  == "rejected"  -> "Pay Rejected" to R.color.red_accent
        a.paymentStatus  == "pending"   -> "Pending Pay"  to R.color.yellow_accent
        a.paymentStatus  == "confirmed" -> "Confirmed"    to R.color.success_green
        a.status         == "confirmed" -> "Confirmed"    to R.color.success_green
        else                            -> "Registered"   to R.color.purple_light
    }

    private fun setupActionButtons(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val eventId = event?.id ?: return
        val rsvpId  = attendee.id
        val isPending = attendee.paymentStatus == "pending"
        row.btnApprove.showIf(isPending)
        row.btnReject.showIf(isPending)

        val isReservedSeating = event?.seatingType == "reserved"
        val canAssignSeat = attendee.paymentStatus == "confirmed" && isReservedSeating && attendee.seatNumber.isNullOrBlank()
        row.btnAssignSeat.showIf(canAssignSeat)

        row.btnApprove.setOnClickListener {
            if (isReservedSeating) promptApproveWithSeat(eventId, rsvpId)
            else AlertDialog.Builder(requireContext())
                    .setTitle("Approve Payment")
                    .setMessage("Approve this payment?")
                    .setPositiveButton("Approve") { _, _ -> presenter.approvePayment(eventId, rsvpId, null) }
                    .setNegativeButton("Cancel", null)
                    .show()
        }

        row.btnReject.setOnClickListener { promptRejectWithReason { reason -> presenter.rejectPayment(eventId, rsvpId, reason) } }
        row.btnAssignSeat.setOnClickListener { promptAssignSeat(eventId, rsvpId) }
    }

    // Dialog helpers omitted for brevity

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

### Payment Proof Screen

### PaymentProofContract.kt
**Path:** `app/src/main/java/com/hangout/app/ui/paymentproof/PaymentProofContract.kt`

```kotlin
package com.hangout.app.ui.paymentproof

import java.io.File

interface PaymentProofContract {
    interface View {
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onSubmitSuccess()
    }

    interface Presenter {
        fun submitPaymentProof(eventId: Long, imageFile: File)
        fun detachView()
    }
}
```

### PaymentProofModel.kt
**Path:** `app/src/main/java/com/hangout/app/ui/paymentproof/PaymentProofModel.kt`

```kotlin
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
                } catch (_: Exception) {
                    "Submission failed"
                }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}
```

### PaymentProofPresenter.kt
**Path:** `app/src/main/java/com/hangout/app/ui/paymentproof/PaymentProofPresenter.kt`

```kotlin
package com.hangout.app.ui.paymentproof

import com.hangout.app.repository.Result
import kotlinx.coroutines.*
import java.io.File

class PaymentProofPresenter(
    private var view: PaymentProofContract.View?,
    private val model: PaymentProofModel
) : PaymentProofContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun submitPaymentProof(eventId: Long, imageFile: File) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.uploadPaymentProof(eventId, imageFile)) {
                is Result.Success -> view?.onSubmitSuccess()
                is Result.Error   -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
```

### PaymentProofBottomSheet.kt
**Path:** `app/src/main/java/com/hangout/app/ui/paymentproof/PaymentProofBottomSheet.kt`

```kotlin
package com.hangout.app.ui.paymentproof

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.BottomSheetPaymentProofBinding
import com.hangout.app.utils.copyUriToCache
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast
import java.io.File

class PaymentProofBottomSheet : BottomSheetDialogFragment(), PaymentProofContract.View {

    private var _binding: BottomSheetPaymentProofBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: PaymentProofContract.Presenter
    private var selectedImageUri: Uri? = null
    private var event: EventItem? = null

    var onSubmitSuccess: (() -> Unit)? = null

    companion object {
        fun newInstance(event: EventItem, onSuccess: (() -> Unit)? = null) =
            PaymentProofBottomSheet().apply {
                this.event = event
                this.onSubmitSuccess = onSuccess
            }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { handleImageSelected(it) }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = File(requireContext().cacheDir, "proof_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            handleImageSelected(Uri.fromFile(file))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPaymentProofBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet))?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }

        presenter = PaymentProofPresenter(this, PaymentProofModel(requireContext()))
        event?.let { bindEvent(it) }

        binding.btnPickGallery.setOnClickListener { openGallery() }
        binding.btnPickCamera.setOnClickListener { openCamera() }
        binding.btnRemoveImage.setOnClickListener { clearImage() }
        binding.btnSubmit.setOnClickListener { handleSubmit() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCopyAccount.setOnClickListener {
            val number = event?.accountNumber ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("account", number))
            toast("Account number copied!")
        }
    }

    private fun bindEvent(e: EventItem) {
        val price = e.price?.toInt() ?: 0
        binding.tvEventTitle.text = e.title ?: "Untitled"
        binding.tvEventPrice.text = "₱${"%,d".format(price)}"
        binding.tvPaymentMethod.text = when (e.paymentMethod?.lowercase()) {
            "gcash" -> "GCash"
            "paymaya", "maya" -> "Maya"
            "bank" -> "Bank Transfer"
            else -> e.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "—"
        }
        binding.tvAccountName.text = e.accountName ?: "—"
        binding.tvAccountNumber.text = e.accountNumber ?: "—"
        if (e.noRefundPolicy == true) binding.layoutNoRefund.show() else binding.layoutNoRefund.hide()
        updateSubmitState()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun handleImageSelected(uri: Uri) {
        selectedImageUri = uri
        Glide.with(this).load(uri).centerCrop().into(binding.ivPreview)
        binding.layoutPreview.show()
        binding.layoutPickButtons.hide()
        updateSubmitState()
    }

    private fun clearImage() {
        selectedImageUri = null
        binding.layoutPreview.hide()
        binding.layoutPickButtons.show()
        updateSubmitState()
    }

    private fun handleSubmit() {
        val uri = selectedImageUri ?: run {
            toast("Please select a proof of payment first.")
            return
        }
        if (event?.noRefundPolicy == true && !binding.cbAcknowledge.isChecked) {
            toast("Please acknowledge the refund policy first.")
            return
        }
        val path = copyUriToCache(requireContext(), uri) ?: run {
            toast("Could not read image. Please try again.")
            return
        }
        presenter.submitPaymentProof(event?.id ?: return, File(path))
    }

    private fun updateSubmitState() {
        val hasImage = selectedImageUri != null
        val policyOk = event?.noRefundPolicy != true || binding.cbAcknowledge.isChecked
        binding.btnSubmit.isEnabled = hasImage && policyOk
        binding.cbAcknowledge.setOnCheckedChangeListener { _, _ -> updateSubmitState() }
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnSubmit.isEnabled = !show
        binding.btnCancel.isEnabled = !show
    }

    override fun showMessage(message: String) = toast(message)

    override fun onSubmitSuccess() {
        toast("Payment proof submitted! Waiting for host approval.")
        onSubmitSuccess?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
```

---

## CUSTOM COMPONENTS

### GradientTextView.kt
**Path:** `app/src/main/java/com/hangout/app/ui/custom/GradientTextView.kt`

```kotlin
package com.hangout.app.ui.custom

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (width > 0) {
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(
                    0xFFFFFFFF.toInt(),  // white
                    0xFF9D5FF5.toInt()   // purple
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }
}
```

---

## RESOURCES

### colors.xml
**Path:** `app/src/main/res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- ── Brand ───────────────────────────────────────────────────────── -->
    <color name="purple_main">#A855F7</color>
    <color name="purple_light">#C084FC</color>
    <color name="purple_dark">#7C3AED</color>

    <!-- ── Backgrounds ─────────────────────────────────────────────────── -->
    <color name="bg_dark">#0F0E1A</color>
    <color name="bg_card">#1E2040</color>

    <!-- ── Text ───────────────────────────────────────────────────────── -->
    <color name="text_primary">#F0EDFF</color>
    <color name="text_muted">#6B6888</color>
    <color name="white">#FFFFFF</color>

    <!-- ── Accents ───────────────────────────────────────────────────────── -->
    <color name="success_green">#22C55E</color>
    <color name="yellow_accent">#EAB308</color>
    <color name="red_accent">#EF4444</color>

    <!-- ── Misc ───────────────────────────────────────────────────────── -->
    <color name="divider">#1F1D30</color>

    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="nav_selected">#A855F7</color>

    <color name="input_bg">#1A1A2E</color>
    <color name="input_border">#2AFFFFFF</color>
    <color name="card_bg">#1E2040</color>

    <color name="text_secondary">#9CA3AF</color>
    <color name="pink_accent">#EC4899</color>

</resources>
```

### strings.xml
**Path:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">HangOut</string>
    <!-- TODO: Remove or change this placeholder text -->
    <string name="hello_blank_fragment">Hello blank fragment</string>
</resources>
```

### styles.xml
**Path:** `app/src/main/res/values/styles.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Base app theme -->
    <style name="Theme.HangOut" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_main</item>
        <item name="colorPrimaryVariant">@color/purple_dark</item>
        <item name="colorAccent">@color/purple_main</item>
        <item name="android:windowBackground">@color/bg_dark</item>
        <item name="android:statusBarColor">@color/bg_dark</item>
        <item name="android:navigationBarColor">@color/bg_dark</item>
        <item name="colorSurface">@color/card_bg</item>
        <item name="android:colorBackground">@color/bg_dark</item>
        <item name="colorBackgroundFloating">@color/card_bg</item>
    </style>

    <!-- TextInputLayout — dark outlined style matching the web app inputs -->
    <style name="HangOutInputLayout" parent="Widget.MaterialComponents.TextInputLayout.OutlinedBox">
        <item name="boxBackgroundColor">@color/input_bg</item>
        <item name="boxStrokeColor">@color/input_border</item>
        <item name="boxStrokeWidth">1.5dp</item>
        <item name="boxCornerRadiusTopStart">10dp</item>
        <item name="boxCornerRadiusTopEnd">10dp</item>
        <item name="boxCornerRadiusBottomStart">10dp</item>
        <item name="boxCornerRadiusBottomEnd">10dp</item>
        <item name="hintTextColor">@color/text_muted</item>
        <item name="android:textColorHint">@color/text_muted</item>
        <item name="boxStrokeErrorColor">@color/red_accent</item>
        <item name="errorTextColor">@color/red_accent</item>
        <item name="passwordToggleTint">@color/text_muted</item>
        <item name="android:editTextColor">@color/text_primary</item>
    </style>

    <!-- Disabled variant for email field in Edit Profile -->
    <style name="HangOutInputLayoutDisabled" parent="HangOutInputLayout">
        <item name="boxStrokeColor">@color/divider</item>
        <item name="boxBackgroundColor">#0DFFFFFF</item>
    </style>

    <style name="AppBottomSheetDialogTheme" parent="Theme.MaterialComponents.DayNight.BottomSheetDialog">
        <item name="bottomSheetStyle">@style/AppBottomSheetStyle</item>
    </style>

    <style name="AppBottomSheetStyle" parent="Widget.MaterialComponents.BottomSheet">
        <item name="android:background">@android:color/transparent</item>
        <item name="backgroundTint">@android:color/transparent</item>
        <item name="android:elevation">0dp</item>
        <item name="elevation">0dp</item>
    </style>

    <style name="DarkEditText" parent="Widget.MaterialComponents.TextInputEditText.OutlinedBox">
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textColorHint">@color/text_muted</item>
        <item name="android:background">@android:color/transparent</item>
    </style>

    <style name="DarkInputTheme">
        <item name="colorOnSurface">@color/text_primary</item>
        <item name="colorOnBackground">@color/text_primary</item>
        <item name="android:textColorHint">@color/text_muted</item>
        <item name="hintTextColor">@color/text_muted</item>
    </style>

</resources>
```

### Drawable Resources

**All drawable files in `app/src/main/res/drawable/`:**

- auth_background.xml
- auth_card_bg.xml
- avatar_bg.xml
- avatar_gradient_bg.xml
- badge_format_bg.xml
- btn_back_bg.xml
- btn_ghost.xml
- btn_glass_bg.xml
- btn_gradient.xml
- btn_gradient_bg.xml
- btn_icon_bg.xml
- btn_maps_bg.xml
- btn_pill_purple.xml
- btn_pill_red.xml
- btn_rsvp_bg.xml
- btn_rsvp_pending_bg.xml
- btn_rsvp_success_bg.xml
- camera_btn_bg.xml
- capacity_progress.xml
- card_bg.xml
- card_dark_bg.xml
- card_info_bg.xml
- cover_photo_bg.xml
- dialog_bg.xml
- divider_bottom.xml
- divider_top.xml
- dot_active.xml
- dot_inactive.xml
- error_bg.xml
- glow_pink.xml
- glow_purple.xml
- hero_gradient_overlay.xml
- ic_add.xml
- ic_back.xml
- ic_bell.xml
- ic_calendar.xml
- ic_camera.xml
- ic_chevron_right.xml
- ic_clock.xml
- ic_close.xml
- ic_credit_card.xml
- ic_external_link.xml
- ic_heart_outline.xml
- ic_home.xml
- ic_info.xml
- ic_launcher_background.xml
- ic_launcher_foreground.xml
- ic_location.xml
- ic_lock.xml
- ic_logout.xml
- ic_mail.xml
- ic_search.xml
- ic_seat.xml
- ic_settings.xml
- ic_share.xml
- ic_tag.xml
- ic_ticket.xml
- ic_trending.xml
- ic_upload.xml
- ic_user.xml
- ic_users.xml
- rsvp_bar_bg.xml
- settings_icon_bg.xml
- stat_bg_pink.xml
- stat_bg_purple.xml
- tab_active_bg.xml
- tab_container_bg.xml
- verified_badge_bg.xml

---

## LAYOUTS

---

## activity_main.xml
**Path:** `app/src/main/res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_dark">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigation" />

    <View
        android:id="@+id/navDivider"
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="@color/divider"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigation"/>

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="76dp"
        android:background="@color/bg_dark"
        app:itemIconTint="@color/nav_item_colors"
        app:itemTextColor="@color/nav_item_colors"
        app:itemIconSize="24dp"
        app:labelVisibilityMode="labeled"
        app:itemActiveIndicatorStyle="@null"
        app:menu="@menu/bottom_nav_menu"
        app:layout_constraintBottom_toBottomOf="parent" />

    <ImageView
        android:id="@+id/fabCreate"
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:background="@drawable/btn_gradient"
        android:src="@drawable/ic_add"
        android:padding="16dp"
        android:elevation="12dp"
        android:clickable="true"
        android:focusable="true"
        app:tint="@color/white"
        app:layout_constraintBottom_toBottomOf="@id/bottomNavigation"
        app:layout_constraintTop_toTopOf="@id/bottomNavigation"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintVertical_bias="0.15" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## activity_auth.xml
**Path:** `app/src/main/res/layout/activity_auth.xml`

[This is a large authentication layout file with sign-in and register forms. See code above for full structure.]

---

## fragment_home.xml
**Path:** `app/src/main/res/layout/fragment_home.xml`

[This layout includes welcome message, stats cards, hosting events, and "happening now" sections.]

---

## fragment_discover.xml
**Path:** `app/src/main/res/layout/fragment_discover.xml`

[This layout includes search bar, filter pills, and event listings.]

---

## fragment_my_hangouts.xml
**Path:** `app/src/main/res/layout/fragment_my_hangouts.xml`

[This layout has tab switching between hosting and attending events.]

---

## fragment_profile.xml
**Path:** `app/src/main/res/layout/fragment_profile.xml`

[This layout includes profile card with avatar, stats, and account settings options.]

---

## fragment_host_dashboard.xml
**Path:** `app/src/main/res/layout/fragment_host_dashboard.xml`

[This layout shows host event summary, attendee list, status counts, and host actions like refresh, cancel, and delete.]

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_dark">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="48dp"
        android:paddingBottom="12dp"
        android:paddingHorizontal="20dp"
        android:background="@color/bg_dark">

        <ImageButton
            android:id="@+id/btnBack"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="@drawable/btn_icon_bg"
            android:src="@drawable/ic_back"
            app:tint="@color/text_primary"
            android:contentDescription="Back"
            android:layout_marginEnd="12dp" />

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Manage HangOut"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <ImageButton
            android:id="@+id/btnRefresh"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="@drawable/btn_icon_bg"
            android:src="@android:drawable/ic_menu_rotate"
            app:tint="@color/text_muted"
            android:contentDescription="Refresh" />
    </LinearLayout>

    <!-- Loading bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="3dp"
        android:indeterminate="true"
        android:visibility="gone"
        android:indeterminateTint="@color/purple_main" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="20dp"
            android:paddingBottom="32dp">

            <!-- Cancelled banner -->
            <TextView
                android:id="@+id/tvCancelledBanner"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="⚠ This event has been cancelled"
                android:textColor="@color/red_accent"
                android:textSize="13sp"
                android:gravity="center"
                android:background="#22F87171"
                android:padding="10dp"
                android:layout_marginTop="8dp"
                android:visibility="gone" />

            <!-- Event info card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginTop="12dp">

                <TextView
                    android:id="@+id/tvEventTitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_primary"
                    android:textSize="16sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="4dp" />

                <TextView
                    android:id="@+id/tvEventMeta"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_muted"
                    android:textSize="13sp"
                    android:layout_marginBottom="2dp" />

                <TextView
                    android:id="@+id/tvEventLocation"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_muted"
                    android:textSize="13sp"
                    android:layout_marginBottom="12dp" />

                <TextView
                    android:id="@+id/tvSlots"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp"
                    android:layout_marginBottom="6dp" />

                <ProgressBar
                    android:id="@+id/progressSlots"
                    style="@style/Widget.AppCompat.ProgressBar.Horizontal"
                    android:layout_width="match_parent"
                    android:layout_height="6dp"
                    android:progress="0"
                    android:progressTint="@color/purple_main"
                    android:progressBackgroundTint="#33A855F7" />
            </LinearLayout>

            <!-- Summary line -->
            <TextView
                android:id="@+id/tvSummary"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/text_muted"
                android:textSize="12sp"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="8dp"
                android:visibility="gone" />

            <!-- Attendees header -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Attendees"
                android:textColor="@color/text_primary"
                android:textSize="15sp"
                android:textStyle="bold"
                android:layout_marginTop="4dp"
                android:layout_marginBottom="8dp" />

            <!-- Attendee rows injected here -->
            <LinearLayout
                android:id="@+id/layoutAttendeesContainer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical" />

            <!-- Danger zone -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Danger Zone"
                android:textColor="@color/red_accent"
                android:textSize="13sp"
                android:textStyle="bold"
                android:layout_marginTop="28dp"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/btnCancelEvent"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Cancel Event"
                android:textSize="14sp"
                android:backgroundTint="#22F87171"
                android:textColor="@color/red_accent"
                android:layout_marginBottom="8dp"
                style="@style/Widget.MaterialComponents.Button" />

            <Button
                android:id="@+id/btnDeleteEvent"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Delete Event"
                android:textSize="14sp"
                android:backgroundTint="@color/red_accent"
                android:textColor="@color/white"
                style="@style/Widget.MaterialComponents.Button" />

        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

---

## bottom_sheet_payment_proof.xml
**Path:** `app/src/main/res/layout/bottom_sheet_payment_proof.xml`

[Bottom sheet layout for payment proof upload, event details, account info, no-refund acknowledgment, image preview, and submit button.]

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/dialog_bg">

    <!-- Handle -->
    <View
        android:layout_width="40dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="12dp"
        android:layout_marginBottom="8dp"
        android:background="#33FFFFFF"
        android:backgroundTint="@null" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="20dp"
            android:paddingBottom="32dp">

            <!-- Header -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Payment Verification"
                android:textColor="@color/text_primary"
                android:textSize="20sp"
                android:textStyle="bold"
                android:layout_marginTop="8dp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Upload your proof of payment to reserve your spot"
                android:textColor="@color/text_muted"
                android:textSize="13sp"
                android:layout_marginTop="4dp"
                android:layout_marginBottom="20dp" />

            <!-- Event info card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/tvEventTitle"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="15sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <TextView
                    android:id="@+id/tvEventPrice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="@color/purple_light"
                    android:textSize="22sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Payment info -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="PAYMENT DETAILS"
                android:textColor="@color/text_muted"
                android:textSize="11sp"
                android:textStyle="bold"
                android:letterSpacing="0.1"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="14dp"
                android:layout_marginBottom="16dp">

                <!-- Method -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginBottom="10dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Method"
                        android:textColor="@color/text_muted"
                        android:textSize="12sp" />

                    <TextView
                        android:id="@+id/tvPaymentMethod"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Account name -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginBottom="10dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Account Name"
                        android:textColor="@color/text_muted"
                        android:textSize="12sp" />

                    <TextView
                        android:id="@+id/tvAccountName"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Account number + copy -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Account Number"
                        android:textColor="@color/text_muted"
                        android:textSize="12sp" />

                    <TextView
                        android:id="@+id/tvAccountNumber"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:layout_marginEnd="8dp" />

                    <Button
                        android:id="@+id/btnCopyAccount"
                        android:layout_width="wrap_content"
                        android:layout_height="32dp"
                        android:text="Copy"
                        android:textSize="11sp"
                        android:backgroundTint="#33A855F7"
                        android:textColor="@color/purple_light"
                        style="@style/Widget.MaterialComponents.Button" />
                </LinearLayout>
            </LinearLayout>

            <!-- Instructions -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_info_bg"
                android:padding="14dp"
                android:layout_marginBottom="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Instructions"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="1. Send the exact amount to the account above\n2. Screenshot your payment confirmation\n3. Upload the screenshot below\n4. Wait for host approval (usually within 24 hrs)"
                    android:textColor="@color/text_muted"
                    android:textSize="12sp"
                    android:lineSpacingExtra="4dp" />
            </LinearLayout>

            <!-- No-refund policy -->
            <LinearLayout
                android:id="@+id/layoutNoRefund"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/error_bg"
                android:padding="14dp"
                android:layout_marginBottom="16dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="No Refund Policy"
                    android:textColor="@color/red_accent"
                    android:textSize="13sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="6dp" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="This event does not offer refunds. By submitting payment, you acknowledge and accept this policy."
                    android:textColor="@color/text_muted"
                    android:textSize="12sp"
                    android:lineSpacingExtra="2dp"
                    android:layout_marginBottom="10dp" />

                <CheckBox
                    android:id="@+id/cbAcknowledge"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="I acknowledge and accept the no refunds policy"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp"
                    android:buttonTint="@color/purple_main" />
            </LinearLayout>

            <!-- Upload section label -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="UPLOAD PROOF OF PAYMENT"
                android:textColor="@color/text_muted"
                android:textSize="11sp"
                android:textStyle="bold"
                android:letterSpacing="0.1"
                android:layout_marginBottom="10dp" />

            <!-- Pick buttons (gallery / camera) -->
            <LinearLayout
                android:id="@+id/layoutPickButtons"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:orientation="horizontal"
                android:gravity="center"
                android:background="@drawable/cover_photo_bg"
                android:layout_marginBottom="12dp">

                <Button
                    android:id="@+id/btnPickGallery"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="📁 Gallery"
                    android:textSize="13sp"
                    android:backgroundTint="@color/bg_card"
                    android:textColor="@color/text_primary"
                    android:layout_marginHorizontal="12dp"
                    style="@style/Widget.MaterialComponents.Button" />

                <Button
                    android:id="@+id/btnPickCamera"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="📷 Camera"
                    android:textSize="13sp"
                    android:backgroundTint="@color/bg_card"
                    android:textColor="@color/text_primary"
                    android:layout_marginHorizontal="12dp"
                    style="@style/Widget.MaterialComponents.Button" />
            </LinearLayout>

            <!-- Preview (hidden until image selected) -->
            <LinearLayout
                android:id="@+id/layoutPreview"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="12dp"
                android:layout_marginBottom="12dp"
                android:visibility="gone">

                <ImageView
                    android:id="@+id/ivPreview"
                    android:layout_width="match_parent"
                    android:layout_height="200dp"
                    android:scaleType="centerCrop"
                    android:background="#11FFFFFF"
                    android:layout_marginBottom="10dp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="✓ Ready to submit"
                        android:textColor="@color/success_green"
                        android:textSize="13sp"
                        android:textStyle="bold" />

                    <Button
                        android:id="@+id/btnRemoveImage"
                        android:layout_width="wrap_content"
                        android:layout_height="36dp"
                        android:text="Remove"
                        android:textSize="12sp"
                        android:backgroundTint="#33F87171"
                        android:textColor="@color/red_accent"
                        style="@style/Widget.MaterialComponents.Button" />
                </LinearLayout>
            </LinearLayout>

            <!-- Progress bar -->
            <ProgressBar
                android:id="@+id/progressBar"
                style="@style/Widget.AppCompat.ProgressBar.Horizontal"
                android:layout_width="match_parent"
                android:layout_height="3dp"
                android:indeterminate="true"
                android:visibility="gone"
                android:indeterminateTint="@color/purple_main"
                android:layout_marginBottom="12dp" />

            <!-- Footer buttons -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="4dp">

                <Button
                    android:id="@+id/btnCancel"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Cancel"
                    android:textSize="14sp"
                    android:backgroundTint="@color/bg_card"
                    android:textColor="@color/text_primary"
                    android:layout_marginEnd="10dp"
                    style="@style/Widget.MaterialComponents.Button" />

                <Button
                    android:id="@+id/btnSubmit"
                    android:layout_width="0dp"
                    android:layout_height="52dp"
                    android:layout_weight="1"
                    android:text="Submit Proof"
                    android:textSize="14sp"
                    android:backgroundTint="@color/purple_main"
                    android:textColor="@color/white"
                    android:enabled="false"
                    style="@style/Widget.MaterialComponents.Button" />
            </LinearLayout>

        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

---

## dialog_payment_proof.xml
**Path:** `app/src/main/res/layout/dialog_payment_proof.xml`

[Dialog layout that displays the uploaded payment proof image in full view.]

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp">

    <ImageView
        android:id="@+id/ivProofImage"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:scaleType="fitCenter"
        android:background="#11FFFFFF" />
</FrameLayout>
```

---

## item_attendee_row.xml
**Path:** `app/src/main/res/layout/item_attendee_row.xml`

[Attendee row card with avatar, attendee details, status badge, seat info, and action buttons for approve/reject/assign seat.]

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/card_bg"
    android:padding="14dp"
    android:layout_marginBottom="8dp">

    <!-- Top row: avatar + name + status -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <ImageView
            android:id="@+id/ivAvatar"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="@drawable/avatar_bg"
            android:src="@drawable/ic_user"
            android:padding="8dp"
            app:tint="@color/text_muted"
            android:layout_marginEnd="12dp" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvAttendeeName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_primary"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvAttendeeEmail"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_muted"
                android:textSize="12sp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:gravity="end">

            <TextView
                android:id="@+id/tvStatusBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="11sp"
                android:textStyle="bold"
                android:layout_marginBottom="2dp" />

            <TextView
                android:id="@+id/tvSeatNumber"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/purple_light"
                android:textSize="11sp"
                android:visibility="gone" />
        </LinearLayout>
    </LinearLayout>

    <!-- Action buttons row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="10dp">

        <Button
            android:id="@+id/btnViewProof"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="View Proof"
            android:textSize="11sp"
            android:backgroundTint="#33A855F7"
            android:textColor="@color/purple_light"
            android:layout_marginEnd="6dp"
            android:visibility="gone"
            style="@style/Widget.MaterialComponents.Button" />

        <Button
            android:id="@+id/btnApprove"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Approve"
            android:textSize="11sp"
            android:backgroundTint="#334ADE80"
            android:textColor="@color/success_green"
            android:layout_marginEnd="6dp"
            android:visibility="gone"
            style="@style/Widget.MaterialComponents.Button" />

        <Button
            android:id="@+id/btnReject"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Reject"
            android:textSize="11sp"
            android:backgroundTint="#33F87171"
            android:textColor="@color/red_accent"
            android:layout_marginEnd="6dp"
            android:visibility="gone"
            style="@style/Widget.MaterialComponents.Button" />

        <Button
            android:id="@+id/btnAssignSeat"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Assign Seat"
            android:textSize="11sp"
            android:backgroundTint="#33A855F7"
            android:textColor="@color/purple_light"
            android:visibility="gone"
            style="@style/Widget.MaterialComponents.Button" />
    </LinearLayout>
</LinearLayout>
```

---

## fragment_attending_dashboard.xml
**Path:** `app/src/main/res/layout/fragment_attending_dashboard.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_dark">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="48dp"
        android:paddingBottom="12dp"
        android:paddingHorizontal="20dp"
        android:background="@color/bg_dark">

        <ImageButton
            android:id="@+id/btnBack"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="@drawable/btn_icon_bg"
            android:src="@drawable/ic_back"
            app:tint="@color/text_primary"
            android:contentDescription="Back"
            android:layout_marginEnd="12dp" />

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="My RSVP"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- Loading bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="3dp"
        android:indeterminate="true"
        android:visibility="gone"
        android:indeterminateTint="@color/purple_main" />

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="20dp"
            android:paddingBottom="40dp">

            <!-- Status stripe + label -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/card_bg"
                android:layout_marginTop="12dp"
                android:layout_marginBottom="12dp">

                <View
                    android:id="@+id/viewStatusStripe"
                    android:layout_width="4dp"
                    android:layout_height="match_parent"
                    android:minHeight="56dp"
                    android:background="@color/purple_main" />

                <TextView
                    android:id="@+id/tvStatusLabel"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:padding="16dp"
                    android:textSize="15sp"
                    android:textStyle="bold"
                    android:textColor="@color/success_green" />
            </LinearLayout>

            <!-- Event info card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="12dp">

                <TextView
                    android:id="@+id/tvEventTitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_primary"
                    android:textSize="17sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="10dp" />

                <TextView
                    android:id="@+id/tvEventDate"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_muted"
                    android:textSize="13sp"
                    android:layout_marginBottom="4dp" />

                <TextView
                    android:id="@+id/tvEventLocation"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_muted"
                    android:textSize="13sp"
                    android:layout_marginBottom="8dp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:id="@+id/tvEventFormat"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/purple_light"
                        android:textSize="12sp"
                        android:background="#22A855F7"
                        android:paddingHorizontal="10dp"
                        android:paddingVertical="4dp"
                        android:layout_marginEnd="8dp" />

                    <TextView
                        android:id="@+id/tvEventPrice"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/success_green"
                        android:textSize="12sp"
                        android:background="#224ADE80"
                        android:paddingHorizontal="10dp"
                        android:paddingVertical="4dp" />
                </LinearLayout>
            </LinearLayout>

            <!-- Ticket info (confirmed only) -->
            <LinearLayout
                android:id="@+id/layoutTicketInfo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="12dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🎟  YOUR TICKET"
                    android:textColor="@color/text_muted"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.1"
                    android:layout_marginBottom="12dp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginBottom="8dp">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Ticket No."
                        android:textColor="@color/text_muted"
                        android:textSize="12sp" />

                    <TextView
                        android:id="@+id/tvTicketNumber"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="13sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Seat"
                        android:textColor="@color/text_muted"
                        android:textSize="12sp" />

                    <TextView
                        android:id="@+id/tvSeatNumber"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/purple_light"
                        android:textSize="13sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>

            <!-- Virtual link (confirmed + virtual only) -->
            <LinearLayout
                android:id="@+id/layoutVirtualLink"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="12dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Virtual Event"
                    android:textColor="@color/text_muted"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:layout_marginBottom="10dp" />

                <Button
                    android:id="@+id/btnJoinMeeting"
                    android:layout_width="match_parent"
                    android:layout_height="44dp"
                    android:text="🔗 Join Meeting"
                    android:textSize="14sp"
                    android:backgroundTint="#22A855F7"
                    android:textColor="@color/purple_light"
                    style="@style/Widget.MaterialComponents.Button" />
            </LinearLayout>

            <!-- Payment status (paid events) -->
            <LinearLayout
                android:id="@+id/layoutPaymentSection"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="12dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="PAYMENT STATUS"
                    android:textColor="@color/text_muted"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.1"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvPaymentStatus"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Refund status -->
            <LinearLayout
                android:id="@+id/layoutRefundSection"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:padding="16dp"
                android:layout_marginBottom="12dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="REFUND STATUS"
                    android:textColor="@color/text_muted"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.1"
                    android:layout_marginBottom="8dp" />

                <TextView
                    android:id="@+id/tvRefundStatus"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Action buttons -->
            <Button
                android:id="@+id/btnUploadProof"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:text="Upload Payment Proof"
                android:textSize="14sp"
                android:backgroundTint="@color/purple_main"
                android:textColor="@color/white"
                android:layout_marginBottom="10dp"
                android:visibility="gone"
                style="@style/Widget.MaterialComponents.Button" />

            <Button
                android:id="@+id/btnAcknowledgeRefund"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:text="Acknowledge Refund Received"
                android:textSize="14sp"
                android:backgroundTint="#33C084FC"
                android:textColor="@color/purple_light"
                android:layout_marginBottom="10dp"
                android:visibility="gone"
                style="@style/Widget.MaterialComponents.Button" />

            <Button
                android:id="@+id/btnRequestRefund"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:text="Request Refund"
                android:textSize="14sp"
                android:backgroundTint="#33FBBF24"
                android:textColor="@color/yellow_accent"
                android:layout_marginBottom="10dp"
                android:visibility="gone"
                style="@style/Widget.MaterialComponents.Button" />

            <Button
                android:id="@+id/btnCancelRsvp"
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:text="Cancel RSVP"
                android:textSize="14sp"
                android:backgroundTint="#33F87171"
                android:textColor="@color/red_accent"
                android:visibility="gone"
                style="@style/Widget.MaterialComponents.Button" />

        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

### fragment_digital_ticket.xml
**Path:** `app/src/main/res/layout/fragment_digital_ticket.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_dark">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="48dp"
        android:paddingBottom="12dp"
        android:paddingHorizontal="20dp"
        android:background="@color/bg_dark">

        <ImageButton
            android:id="@+id/btnBack"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="@drawable/btn_icon_bg"
            android:src="@drawable/ic_back"
            app:tint="@color/text_primary"
            android:contentDescription="Back"
            android:layout_marginEnd="12dp" />

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Digital Ticket"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />
    </LinearLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="20dp"
            android:paddingBottom="40dp">

            <!-- Ticket card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/card_bg"
                android:layout_marginTop="8dp"
                android:clipToPadding="true">

                <!-- Cover image -->
                <ImageView
                    android:id="@+id/ivCoverImage"
                    android:layout_width="match_parent"
                    android:layout_height="160dp"
                    android:scaleType="centerCrop"
                    android:visibility="gone" />

                <!-- Gradient header if no image -->
                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:background="@drawable/hero_gradient_overlay"
                    android:padding="20dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="HangOut"
                        android:textColor="@color/purple_light"
                        android:textSize="12sp"
                        android:textStyle="bold"
                        android:letterSpacing="0.15"
                        android:layout_marginBottom="8dp" />

                    <TextView
                        android:id="@+id/tvEventTitle"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_primary"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="4dp" />

                    <TextView
                        android:id="@+id/tvEventFormat"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/purple_light"
                        android:textSize="11sp"
                        android:background="#33A855F7"
                        android:paddingHorizontal="10dp"
                        android:paddingVertical="3dp" />
                </LinearLayout>

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/divider"
                    android:layout_marginVertical="0dp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical"
                        android:layout_marginEnd="12dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="DATE"
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvEventDate"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textSize="13sp"
                            android:textStyle="bold"
                            android:layout_marginBottom="14dp" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="LOCATION"
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvEventLocation"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textSize="13sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="TIME"
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvEventTime"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textSize="13sp"
                            android:textStyle="bold"
                            android:layout_marginBottom="14dp" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="SEAT"
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvSeatNumber"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/purple_light"
                            android:textSize="13sp"
                            android:textStyle="bold" />
                    </LinearLayout>
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingHorizontal="16dp"
                    android:paddingBottom="12dp">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="ATTENDEE"
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvAttendeeName"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textSize="13sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:gravity="end">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="TICKET NO."
                            android:textColor="@color/text_muted"
                            android:textSize="10sp"
                            android:letterSpacing="0.1"
                            android:gravity="end"
                            android:layout_marginBottom="3dp" />

                        <TextView
                            android:id="@+id/tvTicketNumber"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="@color/text_primary"
                            android:textSize="13sp"
                            android:textStyle="bold"
                            android:gravity="end" />
                    </LinearLayout>
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingHorizontal="8dp">

                    <View
                        android:layout_width="20dp"
                        android:layout_height="20dp"
                        android:background="@color/bg_dark"
                        android:backgroundTint="@null" />

                    <View
                        android:layout_width="0dp"
                        android:layout_height="1dp"
                        android:layout_weight="1"
                        android:background="@color/divider" />

                    <View
                        android:layout_width="20dp"
                        android:layout_height="20dp"
                        android:background="@color/bg_dark"
                        android:backgroundTint="@null" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:gravity="center"
                    android:padding="24dp">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="20dp">

                        <View
                            android:id="@+id/viewCheckinBg"
                            android:layout_width="8dp"
                            android:layout_height="8dp"
                            android:background="@color/text_muted"
                            android:layout_marginEnd="6dp" />

                        <TextView
                            android:id="@+id/tvCheckinStatus"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textSize="12sp"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <ImageView
                        android:id="@+id/ivQrCode"
                        android:layout_width="220dp"
                        android:layout_height="220dp"
                        android:scaleType="fitCenter"
                        android:layout_marginBottom="16dp" />

                    <TextView
                        android:id="@+id/tvQrFallback"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="QR code unavailable"
                        android:textColor="@color/text_muted"
                        android:textSize="13sp"
                        android:visibility="gone"
                        android:layout_marginBottom="16dp" />

                    <TextView
                        android:id="@+id/tvQrLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/text_muted"
                        android:textSize="12sp"
                        android:gravity="center" />

                </LinearLayout>

            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:background="@drawable/card_info_bg"
                android:padding="14dp"
                android:layout_marginTop="16dp"
                android:gravity="center_vertical">

                <ImageView
                    android:layout_width="18dp"
                    android:layout_height="18dp"
                    android:src="@drawable/ic_info"
                    app:tint="@color/purple_light"
                    android:layout_marginEnd="10dp" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Present this QR code at the event entrance. Screenshot this ticket for offline access."
                    android:textColor="@color/text_muted"
                    android:textSize="12sp"
                    android:lineSpacingExtra="2dp" />
            </LinearLayout>

        </LinearLayout>
    </ScrollView>
</LinearLayout>
```

---

## fragment_event_detail.xml
**Path:** `app/src/main/res/layout/fragment_event_detail.xml`

[This layout includes hero image, date/time/location cards, stats, availability progress, about section, host info, payment details, and sticky RSVP bar.]

---

## Item Layouts

### item_event_card.xml
**Path:** `app/src/main/res/layout/item_event_card.xml`

[Full-width event card with image, title, date, location, price and format badge.]

### item_event_card_horizontal.xml
**Path:** `app/src/main/res/layout/item_event_card_horizontal.xml`

[Horizontal scrollable event card (320dp width) for horizontal lists.]

### item_attending_card.xml
**Path:** `app/src/main/res/layout/item_attending_card.xml`

[Card for RSVP'd or hosted events with status stripe, ticket info, and action buttons.]

### item_settings_row.xml
**Path:** `app/src/main/res/layout/item_settings_row.xml`

[Settings row with icon, title, subtitle, and chevron.]

### item_settings_row_danger.xml
**Path:** `app/src/main/res/layout/item_settings_row_danger.xml`

[Danger settings row (red accent) for actions like sign out.]

---

## Dialog Layouts

### dialog_edit_profile.xml
**Path:** `app/src/main/res/layout/dialog_edit_profile.xml`

[Bottom sheet dialog for editing profile with photo, name fields, and save/cancel buttons.]

### dialog_change_password.xml
**Path:** `app/src/main/res/layout/dialog_change_password.xml`

[Bottom sheet dialog for changing password with current, new, and confirm fields.]

---

## END OF FILE

---

This file contains all your Kotlin code organized by feature and all layout XML files.
You can easily copy-paste any section or search within this file for specific code.
To delete this file, just right-click and delete from your file explorer.

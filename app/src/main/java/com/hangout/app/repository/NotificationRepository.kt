package com.hangout.app.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hangout.app.data.NotificationItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.AppCache

class NotificationRepository(private val context: Context) {

    private val api  = RetrofitClient.getApiService(context)
    private val gson = Gson()

    suspend fun getNotifications(forceRefresh: Boolean = false): Result<List<NotificationItem>> {
        val key = AppCache.Keys.NOTIFICATIONS
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { json ->
                val type = object : TypeToken<List<NotificationItem>>() {}.type
                return Result.Success(gson.fromJson(json, type))
            }
        }
        return try {
            val r = api.getNotifications()
            if (r.isSuccessful) {
                val json   = gson.toJson(r.body())
                val type   = object : TypeToken<List<NotificationItem>>() {}.type
                val items  = gson.fromJson<List<NotificationItem>>(json, type) ?: emptyList()
                AppCache.put(context, key, gson.toJson(items), AppCache.TTL.NOTIFICATIONS)
                Result.Success(items)
            } else Result.Error("Failed to load notifications")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun getUnreadCount(forceRefresh: Boolean = false): Result<Int> {
        val key = AppCache.Keys.UNREAD_NOTIF
        if (!forceRefresh) {
            AppCache.get(context, key)?.let { return Result.Success(it.toIntOrNull() ?: 0) }
        }
        return try {
            val r = api.getUnreadNotificationCount()
            if (r.isSuccessful) {
                val count = (r.body()?.get("unreadCount") as? Double)?.toInt() ?: 0
                AppCache.put(context, key, count.toString(), AppCache.TTL.UNREAD)
                Result.Success(count)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun markRead(notificationId: Long): Result<Unit> {
        return try {
            val r = api.markNotificationRead(notificationId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.NOTIFICATIONS)
                AppCache.bust(context, AppCache.Keys.UNREAD_NOTIF)
                Result.Success(Unit)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun markAllRead(): Result<Unit> {
        return try {
            val r = api.markAllNotificationsRead()
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.NOTIFICATIONS)
                AppCache.bust(context, AppCache.Keys.UNREAD_NOTIF)
                Result.Success(Unit)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val r = api.deleteNotification(notificationId)
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.NOTIFICATIONS)
                AppCache.bust(context, AppCache.Keys.UNREAD_NOTIF)
                Result.Success(Unit)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    suspend fun deleteAll(): Result<Unit> {
        return try {
            val r = api.deleteAllNotifications()
            if (r.isSuccessful) {
                AppCache.bust(context, AppCache.Keys.NOTIFICATIONS)
                AppCache.bust(context, AppCache.Keys.UNREAD_NOTIF)
                Result.Success(Unit)
            } else Result.Error("Failed")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

private fun parseError(errorJson: String?): String? {
    if (errorJson.isNullOrBlank()) return null
    return try {
        org.json.JSONObject(errorJson).optString("message").ifBlank { null }
    } catch (e: Exception) { null }
}
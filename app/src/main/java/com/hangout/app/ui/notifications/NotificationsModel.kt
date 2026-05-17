package com.hangout.app.ui.notifications

import android.content.Context
import com.hangout.app.data.NotificationItem
import com.hangout.app.repository.NotificationRepository
import com.hangout.app.repository.Result

class NotificationsModel(context: Context) {
    private val repo = NotificationRepository(context)

    suspend fun getNotifications(): Result<List<NotificationItem>> =
        repo.getNotifications(forceRefresh = true)

    suspend fun markRead(id: Long): Result<Unit> = repo.markRead(id)

    suspend fun markAllRead(): Result<Unit> = repo.markAllRead()

    suspend fun deleteNotification(id: Long): Result<Unit> = repo.deleteNotification(id)

    suspend fun deleteAll(): Result<Unit> = repo.deleteAll()
}
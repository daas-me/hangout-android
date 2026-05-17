package com.hangout.app.ui.notifications

import com.hangout.app.data.NotificationItem

interface NotificationsContract {
    interface View {
        fun showNotifications(items: List<NotificationItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showEmpty()
        fun onNotificationDeleted(id: Long)
        fun onAllDeleted()
    }
    interface Presenter {
        fun load()
        fun markRead(id: Long)
        fun markAllRead()
        fun deleteNotification(id: Long)
        fun deleteAll()
        fun detachView()
    }
}
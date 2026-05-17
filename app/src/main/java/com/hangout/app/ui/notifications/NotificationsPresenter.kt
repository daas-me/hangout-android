package com.hangout.app.ui.notifications

import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class NotificationsPresenter(
    private var view: NotificationsContract.View?,
    private val model: NotificationsModel
) : NotificationsContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun load() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getNotifications()) {
                is Result.Success -> {
                    if (r.data.isEmpty()) view?.showEmpty()
                    else view?.showNotifications(r.data)
                }
                is Result.Error -> view?.showError(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun markRead(id: Long) {
        scope.launch {
            // Wait for the server call to complete before returning
            when (model.markRead(id)) {
                is Result.Success -> {
                    // Read status successfully persisted on server
                }
                is Result.Error -> {
                    // Show error but keep UI updated locally
                    view?.showError("Failed to save read status")
                }
            }
        }
    }

    override fun markAllRead() {
        scope.launch {
            model.markAllRead()
            load()
        }
    }

    override fun deleteNotification(id: Long) {
        scope.launch {
            when (model.deleteNotification(id)) {
                is Result.Success -> view?.onNotificationDeleted(id)
                is Result.Error   -> view?.showError("Failed to delete notification")
            }
        }
    }

    override fun deleteAll() {
        scope.launch {
            when (model.deleteAll()) {
                is Result.Success -> view?.onAllDeleted()
                is Result.Error   -> view?.showError("Failed to delete notifications")
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
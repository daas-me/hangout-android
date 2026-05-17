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

    override fun loadFavorites() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getFavoriteEvents()) {
                is Result.Success -> view?.showFavoriteEvents(r.data)
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

    override fun unfavorite(eventId: Long) {
        scope.launch {
            when (val r = model.unfavorite(eventId)) {
                is Result.Success -> view?.onUnfavoriteSuccess(eventId)
                is Result.Error   -> view?.showError(r.message)
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
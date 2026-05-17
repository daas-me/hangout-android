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

    override fun refreshEvent(eventId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.fetchEvent(eventId)) {
                is Result.Success -> view?.showEvent(r.data)
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

    override fun acknowledgeRefund(eventId: Long, choice: String, reason: String?) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.acknowledgeRefund(eventId, choice, reason)) {
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
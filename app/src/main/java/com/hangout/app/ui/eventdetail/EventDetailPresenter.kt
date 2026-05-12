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
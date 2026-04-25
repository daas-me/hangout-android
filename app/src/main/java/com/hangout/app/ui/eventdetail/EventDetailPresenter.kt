package com.hangout.app.ui.eventdetail

import com.hangout.app.models.EventItem
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
    }

    override fun rsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.rsvp(eventId)) {
                is Result.Success -> {
                    view?.showMessage(r.data.message)
                    view?.onRsvpSuccess()
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun removeRsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.removeRsvp(eventId)) {
                is Result.Success -> {
                    view?.showMessage(r.data.message)
                    view?.onRsvpRemoved()
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
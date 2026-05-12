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
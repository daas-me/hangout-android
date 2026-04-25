package com.hangout.app.ui.eventdetail

import com.hangout.app.models.EventItem

interface EventDetailContract {
    interface View {
        fun showEvent(event: EventItem)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onRsvpSuccess()
        fun onRsvpRemoved()
    }
    interface Presenter {
        fun loadEvent(event: EventItem)
        fun rsvp(eventId: Long)
        fun removeRsvp(eventId: Long)
        fun detachView()
    }
}
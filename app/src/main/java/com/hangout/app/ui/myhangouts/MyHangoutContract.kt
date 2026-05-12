package com.hangout.app.ui.myhangouts

import com.hangout.app.data.EventItem

interface MyHangoutsContract {

    interface View {
        fun showAttendingEvents(events: List<EventItem>)
        fun showHostingEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun onCancelSuccess(eventId: Long)
    }

    interface Presenter {
        fun loadAttending()
        fun loadHosting()
        fun cancelRsvp(eventId: Long)
        fun detachView()
    }
}
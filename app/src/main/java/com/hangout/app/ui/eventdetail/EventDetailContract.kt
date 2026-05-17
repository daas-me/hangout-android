package com.hangout.app.ui.eventdetail

import com.hangout.app.data.EventItem

interface EventDetailContract {
    interface View {
        fun showEvent(event: EventItem)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onRsvpSuccess()
        fun onRsvpRemoved()
        fun onRsvpCancelled(isPaid: Boolean)
        fun onRsvpStatusLoaded(isRsvped: Boolean, paymentStatus: String?)
        fun onFavoriteStatusLoaded(isFavorite: Boolean)
        fun onFavoriteToggled(isFavorite: Boolean)
    }
    interface Presenter {
        fun loadEvent(event: EventItem)
        fun checkRsvpStatus(eventId: Long)
        fun rsvp(eventId: Long)
        fun removeRsvp(eventId: Long, reason: String = "")
        fun checkFavoriteStatus(eventId: Long)
        fun toggleFavorite(eventId: Long, currentlyFavorited: Boolean)
        fun detachView()
    }
}
package com.hangout.app.ui.attendingdashboard

import com.hangout.app.data.EventItem

interface AttendingDashboardContract {

    interface View {
        fun showEvent(event: EventItem)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onCancelSuccess()
        fun onRefundRequested()
        fun onRefundAcknowledged()
    }

    interface Presenter {
        fun loadEvent(event: EventItem)
        fun cancelRsvp(eventId: Long)
        fun requestRefund(eventId: Long, reason: String)
        fun acknowledgeRefund(eventId: Long)
        fun detachView()
    }
}
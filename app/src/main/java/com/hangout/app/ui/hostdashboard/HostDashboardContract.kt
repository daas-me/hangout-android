package com.hangout.app.ui.hostdashboard

import com.hangout.app.data.AttendeeItem
import com.hangout.app.data.EventItem

interface HostDashboardContract {

    interface View {
        fun showEvent(event: EventItem)
        fun showAttendees(attendees: List<AttendeeItem>)
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onActionSuccess(rsvpId: Long, newStatus: String)
        fun onEventCancelled()
        fun onEventDeleted()
    }

    interface Presenter {
        fun loadAttendees(eventId: Long)
        fun approvePayment(eventId: Long, rsvpId: Long, seatNumber: String?)
        fun rejectPayment(eventId: Long, rsvpId: Long, reason: String)
        fun assignSeat(eventId: Long, rsvpId: Long, seatNumber: String)
        fun confirmAttendee(eventId: Long, rsvpId: Long)
        fun rejectAttendee(eventId: Long, rsvpId: Long, reason: String)
        fun cancelEvent(eventId: Long, reason: String)
        fun deleteEvent(eventId: Long)
        fun detachView()
    }
}
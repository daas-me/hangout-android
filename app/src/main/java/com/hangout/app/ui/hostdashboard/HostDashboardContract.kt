package com.hangout.app.ui.hostdashboard

import android.net.Uri
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
        fun markAttendance(eventId: Long, rsvpId: Long, status: String)
        fun cancelEvent(eventId: Long, reason: String)
        fun deleteEvent(eventId: Long)

        /**
         * Host marks a refund as processed.
         * @param note      Optional reference note shown to the attendee (e.g. "GCash ref 12345").
         * @param proofUri  URI of the refund-proof image selected from the device.
         *                  Required for refundable paid events; null for events without proof upload.
         */
        fun approveRefund(eventId: Long, rsvpId: Long, note: String, proofUri: Uri?)

        fun rejectRefund(eventId: Long, rsvpId: Long, reason: String)
        fun detachView()
    }
}
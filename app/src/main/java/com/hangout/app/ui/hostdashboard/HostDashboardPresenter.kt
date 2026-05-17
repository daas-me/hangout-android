package com.hangout.app.ui.hostdashboard

import android.net.Uri
import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class HostDashboardPresenter(
    private var view: HostDashboardContract.View?,
    private val model: HostDashboardModel
) : HostDashboardContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAttendees(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getAttendees(eventId, forceRefresh = true)) {
                is Result.Success -> view?.showAttendees(r.data)
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun markAttendance(eventId: Long, rsvpId: Long, status: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.markAttendance(eventId, rsvpId, status)) {
                is Result.Success -> {
                    view?.showMessage("Attendance marked as $status.")
                    view?.onActionSuccess(rsvpId, status)
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun approvePayment(eventId: Long, rsvpId: Long, seatNumber: String?) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.approvePayment(eventId, rsvpId, seatNumber)) {
                is Result.Success -> {
                    view?.showMessage("Payment approved!")
                    view?.onActionSuccess(rsvpId, "confirmed")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun rejectPayment(eventId: Long, rsvpId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.rejectPayment(eventId, rsvpId, reason)) {
                is Result.Success -> {
                    view?.showMessage("Payment rejected.")
                    view?.onActionSuccess(rsvpId, "rejected")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun assignSeat(eventId: Long, rsvpId: Long, seatNumber: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.assignSeat(eventId, rsvpId, seatNumber)) {
                is Result.Success -> {
                    view?.showMessage("Seat $seatNumber assigned.")
                    view?.onActionSuccess(rsvpId, "seat_assigned")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun confirmAttendee(eventId: Long, rsvpId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.confirmAttendee(eventId, rsvpId)) {
                is Result.Success -> {
                    view?.showMessage("Attendee confirmed.")
                    view?.onActionSuccess(rsvpId, "attendee_confirmed")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun rejectAttendee(eventId: Long, rsvpId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.rejectAttendee(eventId, rsvpId, reason)) {
                is Result.Success -> {
                    view?.showMessage("Attendee rejected.")
                    view?.onActionSuccess(rsvpId, "attendee_rejected")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    // ── Refund ─────────────────────────────────────────────────

    /**
     * @param proofUri  URI of the refund-proof image; required for refundable paid events.
     */
    override fun approveRefund(eventId: Long, rsvpId: Long, note: String, proofUri: Uri?) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.approveRefund(eventId, rsvpId, note, proofUri)) {
                is Result.Success -> {
                    view?.showMessage("Refund marked as processed. Attendee will acknowledge receipt.")
                    view?.onActionSuccess(rsvpId, "refund_approved")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun rejectRefund(eventId: Long, rsvpId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.rejectRefund(eventId, rsvpId, reason)) {
                is Result.Success -> {
                    view?.showMessage("Refund request declined.")
                    view?.onActionSuccess(rsvpId, "refund_rejected")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    // ── Event lifecycle ────────────────────────────────────────

    override fun cancelEvent(eventId: Long, reason: String) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.cancelEvent(eventId, reason)) {
                is Result.Success -> view?.onEventCancelled()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun deleteEvent(eventId: Long) {
        scope.launch {
            view?.showLoading(true)
            when (val r = model.deleteEvent(eventId)) {
                is Result.Success -> view?.onEventDeleted()
                is Result.Error   -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
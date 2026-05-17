package com.hangout.app.ui.ticketscanner

import com.hangout.app.data.TicketVerifyResult
import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class TicketScannerPresenter(
    private var view: TicketScannerContract.View?,
    private val model: TicketScannerModel
) : TicketScannerContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastVerifyResult: TicketVerifyResult? = null

    override fun verifyTicket(eventId: Long, ticketToken: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.verifyTicket(eventId, ticketToken)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    if (r.data.valid) {
                        lastVerifyResult = r.data
                        // Check if ticket is already attended
                        val isAlreadyAttended = r.data.attendeeStatus == "attended"
                        if (isAlreadyAttended) {
                            view?.onVerifyFailed("Ticket is already invalid — attendee has already been checked in")
                        } else {
                            // Auto-mark attendance when scanning valid ticket (if not already attended)
                            val rsvpId = r.data.rsvpId ?: run {
                                view?.onVerifyFailed("Ticket information incomplete")
                                return@launch
                            }
                            markAttendance(eventId, rsvpId, "attended")
                        }
                    } else {
                        view?.onVerifyFailed(r.data.message ?: "Invalid ticket")
                    }
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.onVerifyFailed(r.message)
                }
            }
        }
    }

    override fun markAttendance(eventId: Long, rsvpId: Long, status: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.markAttendance(eventId, rsvpId, status)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    // Update the verify result with new status and show success
                    lastVerifyResult?.let { result ->
                        val updatedResult = result.copy(
                            checkInStatus = status,
                            attendeeStatus = status
                        )
                        view?.onVerifySuccess(updatedResult)
                    } ?: run {
                        view?.showMessage("Attendance marked as $status")
                    }
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.onVerifyFailed(r.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
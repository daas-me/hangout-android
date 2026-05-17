package com.hangout.app.ui.ticketscanner

import com.hangout.app.data.TicketVerifyResult

interface TicketScannerContract {

    interface View {
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onVerifySuccess(result: TicketVerifyResult)
        fun onVerifyFailed(message: String)
        fun onAttendanceMarked(result: TicketVerifyResult)
        fun resetScanner()
    }

    interface Presenter {
        fun verifyTicket(eventId: Long, ticketToken: String)
        fun markAttendance(eventId: Long, rsvpId: Long, status: String)
        fun detachView()
    }
}
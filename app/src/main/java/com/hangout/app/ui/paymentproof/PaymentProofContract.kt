package com.hangout.app.ui.paymentproof

import java.io.File

interface PaymentProofContract {
    interface View {
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onSubmitSuccess()
    }
    interface Presenter {
        fun submitPaymentProof(eventId: Long, imageFile: File)
        fun detachView()
    }
}
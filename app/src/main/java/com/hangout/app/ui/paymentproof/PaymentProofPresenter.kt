package com.hangout.app.ui.paymentproof

import com.hangout.app.repository.Result
import kotlinx.coroutines.*
import java.io.File

class PaymentProofPresenter(
    private var view: PaymentProofContract.View?,
    private val model: PaymentProofModel
) : PaymentProofContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun submitPaymentProof(eventId: Long, imageFile: File) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.uploadPaymentProof(eventId, imageFile)) {
                is Result.Success -> view?.onSubmitSuccess()
                is Result.Error   -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
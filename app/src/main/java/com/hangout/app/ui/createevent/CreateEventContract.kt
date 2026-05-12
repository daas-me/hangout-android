package com.hangout.app.ui.createevent

import com.hangout.app.data.CreateEventFormState

interface CreateEventContract {

    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun onEventCreated(eventId: Long, isDraft: Boolean)
        fun updateStepIndicator(currentStep: Int, totalSteps: Int)
    }

    interface Presenter {
        fun saveDraft(state: CreateEventFormState, coverImagePath: String?)
        fun publishEvent(state: CreateEventFormState, coverImagePath: String?)
        fun detachView()
    }
}
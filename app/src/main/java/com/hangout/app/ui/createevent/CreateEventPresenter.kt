package com.hangout.app.ui.createevent

import com.hangout.app.data.CreateEventFormState
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.data.EventItem
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class CreateEventPresenter(
    private var view: CreateEventContract.View?,
    private val model: CreateEventModel,
    private val context: android.content.Context
): CreateEventContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Store loaded event for access from Activity
    var loadedEvent: EventItem? = null

    // ── Public API ─────────────────────────────────────────────────────────

    override fun saveDraft(state: CreateEventFormState, coverImagePath: String?) {
        submit(state, coverImagePath, isDraft = true)
    }

    override fun publishEvent(state: CreateEventFormState, coverImagePath: String?) {
        val error = validateForPublish(state)
        if (error != null) {
            view?.showError(error)
            return
        }
        // Check profile completion before publishing
        checkProfileCompletionForPublish(state, coverImagePath)
    }

    private fun checkProfileCompletionForPublish(state: CreateEventFormState, coverImagePath: String?) {
        view?.showLoading(true)
        scope.launch {
            try {
                val api = com.hangout.app.network.RetrofitClient.getApiService(context)
                val response = api.getProfile()
                view?.showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    if (!isProfileComplete(profile)) {
                        view?.showError("Complete your profile to publish events. All fields are required: First name, Last name, Email, Age, Birthdate, Street/Barangay, Municipality/City, State/Province, Country, and Zip Code.")
                        return@launch
                    }
                    // Profile is complete, proceed with publishing
                    submit(state, coverImagePath, isDraft = false)
                } else {
                    view?.showError("Unable to verify profile. Please try again.")
                }
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showError("Cannot connect to server.")
            }
        }
    }

    private fun isProfileComplete(profile: com.hangout.app.data.UserProfile): Boolean {
        // Check all 10 required fields
        val ageVal = when (val a = profile.age) {
            is Int    -> a
            is Double -> a.toInt()
            is String -> a.toIntOrNull()
            else      -> null
        }
        val hasValidAge = ageVal != null && ageVal > 0
        
        return profile.firstname.isNotBlank() &&
                profile.lastname.isNotBlank() &&
                !profile.email.isNullOrBlank() &&
                hasValidAge &&
                !profile.birthdate.isNullOrBlank() &&
                !profile.street.isNullOrBlank() &&
                !profile.city.isNullOrBlank() &&
                !profile.state.isNullOrBlank() &&
                !profile.country.isNullOrBlank() &&
                !profile.zipcode.isNullOrBlank()
    }

    override fun loadEventForEdit(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val result = model.loadEventForEdit(eventId)) {
                is Result.Success -> {
                    loadedEvent = result.data
                    view?.showLoading(false)
                    view?.onEventLoaded(eventId)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun unpublishEvent(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            val reason = "Event unpublished by host."
            when (val result = model.unpublishEvent(eventId, reason)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showSuccess("Event unpublished successfully")
                    view?.onEventUnpublished()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }

    // ── Private ────────────────────────────────────────────────────────────

    private fun submit(state: CreateEventFormState, coverImagePath: String?, isDraft: Boolean) {
        val draftError = validateMinimumForDraft(state)
        if (draftError != null) {
            view?.showError(draftError)
            return
        }

        view?.showLoading(true)
        scope.launch {
            val request = buildRequest(state, isDraft)
            when (val result = model.createEvent(request)) {
                is Result.Success -> {
                    val eventId = result.data.id
                    AppCache.bust(context, AppCache.Keys.HOSTING_EVENTS)
                    AppCache.bust(context, AppCache.Keys.STATS)

                    // Phase 2: Upload cover image if provided
                    if (coverImagePath != null) {
                        val file = File(coverImagePath)
                        if (file.exists()) {
                            model.uploadCoverImage(eventId, file)
                            // Non-fatal if upload fails — event already created
                        }
                    }

                    view?.showLoading(false)
                    view?.showSuccess(
                        if (isDraft) "Draft saved!" else "Event published successfully!"
                    )
                    view?.onEventCreated(eventId, isDraft)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /** Minimum required to even save a draft. */
    private fun validateMinimumForDraft(state: CreateEventFormState): String? {
        if (state.title.isBlank()) return "Event title is required"
        return null
    }

    /** Full validation required before publishing. */
    private fun validateForPublish(state: CreateEventFormState): String? {
        if (state.title.isBlank())       return "Event title is required"
        if (state.description.isBlank()) return "Description is required"
        if (state.date.isBlank())        return "Event date is required"
        if (state.startTime.isBlank())   return "Start time is required"

        when (state.format) {
            "In-Person", "Hybrid" ->
                if (state.location.isBlank()) return "Location is required for ${state.format} events"
            "Virtual", "Hybrid" ->
                if (state.virtualLink.isBlank()) return "Virtual meeting link is required"
        }

        val cap = state.capacity.toIntOrNull()
        if (cap == null || cap <= 0) return "Capacity must be a positive number"

        if (state.eventType == "paid") {
            val price = state.price.toIntOrNull()
            if (price == null || price <= 0) return "Price must be a positive number for paid events"
            if (state.paymentMethod.isBlank()) return "Payment method is required for paid events"
            if (state.accountNumber.isBlank()) return "Account number is required for paid events"
        }

        return null
    }

    // ── Mapping ────────────────────────────────────────────────────────────

    private fun buildRequest(state: CreateEventFormState, isDraft: Boolean): CreateEventRequest {
        val isPaid = state.eventType == "paid"
        return CreateEventRequest(
            title           = state.title.trim(),
            description     = state.description.trim(),
            date            = state.date,
            startTime       = state.startTime,
            endTime         = state.endTime.ifBlank { null },
            location        = state.location.trim().ifBlank { "TBD" },
            format          = state.format,
            eventType       = state.eventType,
            price           = if (isPaid) (state.price.toIntOrNull() ?: 0) else 0,
            capacity        = state.capacity.toIntOrNull() ?: 0,
            seatingType     = state.seatingType,
            paymentMethod   = if (isPaid) state.paymentMethod.ifBlank { null } else null,
            accountName     = if (isPaid) state.accountName.ifBlank  { null } else null,
            accountNumber   = if (isPaid) state.accountNumber.ifBlank{ null } else null,
            virtualPlatform = if (state.format != "In-Person") state.virtualPlatform.ifBlank { null } else null,
            virtualLink     = if (state.format != "In-Person") state.virtualLink.ifBlank     { null } else null,
            noRefundPolicy  = state.noRefundPolicy,
            isDraft         = isDraft
        )
    }
}
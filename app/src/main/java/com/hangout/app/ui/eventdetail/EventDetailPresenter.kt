package com.hangout.app.ui.eventdetail

import com.hangout.app.data.EventItem
import com.hangout.app.repository.Result
import com.hangout.app.utils.EventHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.cert.CertPathValidatorException.BasicReason

class EventDetailPresenter(
    private var view: EventDetailContract.View?,
    private val model: EventDetailModel
) : EventDetailContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var favoriteCount = 0
    private companion object {
        const val MAX_FAVORITES = 10
    }

    override fun loadEvent(event: EventItem) {
        // Fetch full event details to ensure complete host information is loaded
        event.id?.let {
            scope.launch {
                when (val r = model.getEventDetails(it)) {
                    is Result.Success -> {
                        val fullEvent = r.data
                        // Update EventHolder with complete event data
                        EventHolder.currentEvent = fullEvent
                        view?.showEvent(fullEvent)
                        checkRsvpStatus(it)
                        checkFavoriteStatus(it)
                    }
                    is Result.Error -> {
                        // Fallback to the event passed in if fetch fails
                        view?.showEvent(event)
                        event.id?.let { eventId ->
                            checkRsvpStatus(eventId)
                            checkFavoriteStatus(eventId)
                        }
                    }
                }
            }
        } ?: run {
            // No event ID, use the one we have
            view?.showEvent(event)
        }
    }

    override fun checkRsvpStatus(eventId: Long) {
        scope.launch {
            when (val r = model.checkRsvpStatus(eventId)) {
                is Result.Success -> {
                    val isRsvped = r.data.rsvped &&
                            r.data.status != "cancelled" &&
                            r.data.status != "rejected"
                    view?.onRsvpStatusLoaded(isRsvped, r.data.paymentStatus)
                }
                is Result.Error -> view?.onRsvpStatusLoaded(false, null)
            }
        }
    }

    override fun rsvp(eventId: Long) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.rsvp(eventId)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showMessage("RSVP confirmed!")
                    view?.onRsvpSuccess()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun removeRsvp(eventId: Long, reason: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.removeRsvp(eventId, reason)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showMessage("RSVP cancelled.")
                    val isPaid = (EventHolder.currentEvent?.price ?: 0.0) > 0.0
                    view?.onRsvpCancelled(isPaid)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showMessage(r.message)
                }
            }
        }
    }

    override fun checkFavoriteStatus(eventId: Long) {
        scope.launch {
            when (val r = model.checkFavorite(eventId)) {
                is Result.Success -> {
                    favoriteCount = r.data.favoriteCount ?: 0
                    view?.onFavoriteStatusLoaded(r.data.isFavorite)
                }
                is Result.Error   -> view?.onFavoriteStatusLoaded(false)
            }
        }
    }

    override fun toggleFavorite(eventId: Long, currentlyFavorited: Boolean) {
        // Check if user is trying to add a favorite when already at max limit
        if (!currentlyFavorited && favoriteCount >= MAX_FAVORITES) {
            view?.showMessage("You can only add up to $MAX_FAVORITES favorites")
            return
        }

        scope.launch {
            val result = if (currentlyFavorited)
                model.removeFavorite(eventId)
            else
                model.addFavorite(eventId)

            when (result) {
                is Result.Success -> {
                    if (!currentlyFavorited) {
                        favoriteCount++
                    } else {
                        favoriteCount = (favoriteCount - 1).coerceAtLeast(0)
                    }
                    view?.onFavoriteToggled(!currentlyFavorited)
                }
                is Result.Error   -> view?.showMessage(result.message)
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
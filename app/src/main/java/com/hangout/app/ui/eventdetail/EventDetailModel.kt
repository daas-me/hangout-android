package com.hangout.app.ui.eventdetail

import android.content.Context
import com.hangout.app.data.EventItem
import com.hangout.app.data.FavoriteStatusResponse
import com.hangout.app.data.RsvpResponse
import com.hangout.app.data.RsvpStatusResponse
import com.hangout.app.data.MessageResponse
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

data class CancelRsvpRequest(val reason: String)

class EventDetailModel(context: Context) {

    private val repo = EventRepository(context)

    suspend fun getEventDetails(eventId: Long): Result<EventItem> =
        repo.getEventDetails(eventId)

    suspend fun checkRsvpStatus(eventId: Long): Result<RsvpStatusResponse> =
        repo.checkRsvpStatus(eventId)

    suspend fun rsvp(eventId: Long): Result<RsvpResponse> =
        repo.rsvpEvent(eventId)

    suspend fun removeRsvp(eventId: Long, reason: String): Result<MessageResponse> =
        repo.cancelRsvp(eventId, CancelRsvpRequest(reason = reason.ifBlank { "No reason provided." }))

    suspend fun checkFavorite(eventId: Long): Result<FavoriteStatusResponse> =
        repo.checkFavorite(eventId)

    suspend fun addFavorite(eventId: Long): Result<MessageResponse> =
        repo.addFavorite(eventId)

    suspend fun removeFavorite(eventId: Long): Result<MessageResponse> =
        repo.removeFavorite(eventId)
}
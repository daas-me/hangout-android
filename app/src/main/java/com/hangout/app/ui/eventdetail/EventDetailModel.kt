package com.hangout.app.ui.eventdetail

import android.content.Context
import com.hangout.app.data.RsvpResponse
import com.hangout.app.data.RsvpStatusResponse
import com.hangout.app.data.MessageResponse
import com.hangout.app.repository.EventRepository
import com.hangout.app.repository.Result

class EventDetailModel(context: Context) {

    private val repo = EventRepository(context)

    suspend fun checkRsvpStatus(eventId: Long): Result<RsvpStatusResponse> =
        repo.checkRsvpStatus(eventId)

    suspend fun rsvp(eventId: Long): Result<RsvpResponse> =
        repo.rsvpEvent(eventId)

    suspend fun removeRsvp(eventId: Long): Result<MessageResponse> =
        repo.cancelRsvp(eventId)
}
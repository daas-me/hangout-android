package com.hangout.app.ui.eventdetail

import android.content.Context
import com.hangout.app.models.MessageResponse
import com.hangout.app.repository.Result

class EventDetailModel(context: Context) {
    // Extend UserRepository or add an EventRepository when RSVP endpoints exist
    // For now exposing a stub — swap body for real API call when ready

    suspend fun rsvp(eventId: Long): Result<MessageResponse> {
        // TODO: return repo.rsvp(eventId)
        return Result.Success(MessageResponse("RSVP confirmed!"))
    }

    suspend fun removeRsvp(eventId: Long): Result<MessageResponse> {
        // TODO: return repo.removeRsvp(eventId)
        return Result.Success(MessageResponse("RSVP removed."))
    }
}
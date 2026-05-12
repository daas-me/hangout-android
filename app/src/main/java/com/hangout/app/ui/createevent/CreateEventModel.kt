package com.hangout.app.ui.createevent

import android.content.Context
import com.hangout.app.data.CreateEventRequest
import com.hangout.app.data.CreateEventResponse
import com.hangout.app.repository.CreateEventRepository
import com.hangout.app.repository.Result
import java.io.File

class CreateEventModel(context: Context) {

    private val repo = CreateEventRepository(context)

    suspend fun createEvent(request: CreateEventRequest): Result<CreateEventResponse> =
        repo.createEvent(request)

    suspend fun uploadCoverImage(eventId: Long, imageFile: File): Result<String> =
        repo.uploadCoverImage(eventId, imageFile)
}
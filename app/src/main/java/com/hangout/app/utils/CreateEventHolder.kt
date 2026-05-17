package com.hangout.app.utils

import com.hangout.app.data.CreateEventFormState

/**
 * Holder for CreateEventFormState to avoid passing large objects through Intent
 * (which has a ~1MB parcel size limit).
 */
object CreateEventHolder {
    var formState: CreateEventFormState? = null
    var coverImagePath: String? = null

    fun clear() {
        formState = null
        coverImagePath = null
    }
}

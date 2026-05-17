package com.hangout.app.data

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the JSON the backend returns from:
 *   GET /api/events/{eventId}/rsvp/verify/{ticketToken}
 */
data class TicketVerifyResult(
    val valid: Boolean = false,
    val message: String? = null,

    @SerializedName("attendeeName")  val attendeeName: String? = null,
    @SerializedName("attendeeEmail") val attendeeEmail: String? = null,
    @SerializedName("ticketNumber")  val ticketNumber: String? = null,
    @SerializedName("ticketToken")   val ticketToken: String? = null,
    @SerializedName("seatNumber")    val seatNumber: String? = null,
    @SerializedName("checkInStatus") val checkInStatus: String? = null,
    @SerializedName("eventTitle")    val eventTitle: String? = null,
    @SerializedName("eventDate")     val eventDate: String? = null,
    @SerializedName("attendeePhoto") val attendeePhoto: String? = null,
    @SerializedName("alreadyCheckedIn") val alreadyCheckedIn: Boolean = false,
    @SerializedName("rsvpId")        val rsvpId: Long? = null,
    @SerializedName("attendeeStatus") val attendeeStatus: String? = null  // confirmed | attended | no_show | rejected
)
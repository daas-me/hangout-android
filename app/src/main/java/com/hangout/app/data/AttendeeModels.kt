package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class AttendeeItem(
    val id: Long,                          // RSVP id
    @SerializedName("userId")              val userId: Long,
    @SerializedName("name")               val name: String?,
    @SerializedName("firstname")          val firstName: String?,
    @SerializedName("lastname")           val lastName: String?,
    @SerializedName("email")               val email: String?,
    @SerializedName("photo")               val photo: String?,
    val status: String?,                   // registered | confirmed | cancelled
    @SerializedName("paymentStatus")       val paymentStatus: String?,   // pending | confirmed | rejected
    @SerializedName("paymentProofUrl")     val paymentProofUrl: String?,
    @SerializedName("refundStatus")        val refundStatus: String?,    // pending | waiting_acknowledgement | completed | rejected
    @SerializedName("attendeeStatus")      val attendeeStatus: String?,  // confirmed | rejected | attended | no_show
    @SerializedName("seatNumber")          val seatNumber: String?,
    @SerializedName("ticketToken")         val ticketToken: String?,
    @SerializedName("registeredAt")        val registeredAt: String?,

    // ── Cancellation ──────────────────────────────────────────
    // The reason the attendee provided when they cancelled their RSVP.
    // Sent to backend as "message" in the cancel-RSVP body; returned here
    // so the host can see it in the Refund Requests tab.
    @SerializedName("cancellationReason") val cancellationReason: String?,

    // ── Personal Information ───────────────────────────────────
    @SerializedName("age")                 val age: Any? = null,
    @SerializedName("birthdate")           val birthdate: String? = null,
    @SerializedName("phone")               val phone: String? = null,
    @SerializedName("city")                val city: String? = null,
    @SerializedName("state")               val state: String? = null,
    @SerializedName("country")             val country: String? = null,
    @SerializedName("bio")                 val bio: String? = null,
    @SerializedName("gender")              val gender: String? = null,
    @SerializedName("street")              val street: String? = null,
)

data class ApprovePaymentRequest(
    @SerializedName("seatNumber") val seatNumber: String? = null
)

data class RejectPaymentRequest(
    val reason: String
)

data class AssignSeatRequest(
    @SerializedName("seatNumber") val seatNumber: String
)
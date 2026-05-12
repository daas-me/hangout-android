package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class AttendeeItem(
    val id: Long,                          // RSVP id
    @SerializedName("userId")      val userId: Long,
    @SerializedName("firstName")   val firstName: String?,
    @SerializedName("lastName")    val lastName: String?,
    @SerializedName("email")       val email: String?,
    @SerializedName("photo")       val photo: String?,
    val status: String?,                   // registered | confirmed | cancelled
    @SerializedName("paymentStatus")   val paymentStatus: String?,   // pending | confirmed | rejected
    @SerializedName("paymentProofUrl") val paymentProofUrl: String?,
    @SerializedName("refundStatus")    val refundStatus: String?,
    @SerializedName("attendeeStatus")  val attendeeStatus: String?,  // confirmed | rejected | attended | no_show
    @SerializedName("seatNumber")      val seatNumber: String?,
    @SerializedName("ticketToken")     val ticketToken: String?,
    @SerializedName("registeredAt")    val registeredAt: String?
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
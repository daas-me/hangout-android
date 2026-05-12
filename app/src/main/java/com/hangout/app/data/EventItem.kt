package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class RsvpResponse(
    val id: Long?,
    val status: String?,
    @SerializedName("paymentStatus") val paymentStatus: String?,
    val rsvped: Boolean = true,
    val message: String?
)

data class RsvpStatusResponse(
    val rsvped: Boolean,
    @SerializedName("paymentStatus") val paymentStatus: String?,
    val status: String?
)

data class EventItem(
    val id: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    @SerializedName("startTime")       val startTime: String? = null,
    @SerializedName("endTime")         val endTime: String? = null,
    val location: String? = null,
    val format: String? = null,
    @SerializedName("eventType")       val eventType: String? = null,
    val price: Double? = null,
    val capacity: Int? = null,
    @SerializedName("attendeeCount")   val attendeeCount: Int? = null,
    @SerializedName("seatingType")     val seatingType: String? = null,
    @SerializedName("imageUrl")        val imageUrl: String? = null,
    @SerializedName("paymentMethod")   val paymentMethod: String? = null,
    @SerializedName("accountName")     val accountName: String? = null,
    @SerializedName("accountNumber")   val accountNumber: String? = null,
    @SerializedName("virtualPlatform") val virtualPlatform: String? = null,
    @SerializedName("virtualLink")     val virtualLink: String? = null,
    @SerializedName("noRefundPolicy")  val noRefundPolicy: Boolean? = null,
    @SerializedName("isDraft")         val isDraft: Boolean? = null,
    @SerializedName("eventStatus")     val eventStatus: String? = null,
    @SerializedName("hostId")          val hostId: Long? = null,
    @SerializedName("hostFirstName")   val hostFirstName: String? = null,
    @SerializedName("hostLastName")    val hostLastName: String? = null,
    @SerializedName("hostEmail")       val hostEmail: String? = null,
    @SerializedName("hostPhoto")       val hostPhoto: String? = null,
    val status: String? = null,
    @SerializedName("paymentStatus")   val rsvpPaymentStatus: String? = null,
    val seatNumber: String? = null,
    val ticketNumber: String? = null,
    @SerializedName("ticketToken") val ticketToken: String? = null,
    @SerializedName("refundStatus")    val refundStatus: String? = null,
    @SerializedName("attendeeStatus")  val attendeeStatus: String? = null
)
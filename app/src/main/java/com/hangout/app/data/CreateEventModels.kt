package com.hangout.app.data

import com.google.gson.annotations.SerializedName

// ── Request body sent to POST /api/events ────────────────────────────────────

data class CreateEventRequest(
    val title: String,
    val description: String,
    val date: String,           // "YYYY-MM-DD"
    @SerializedName("startTime") val startTime: String,    // "HH:mm"
    @SerializedName("endTime")   val endTime: String?,     // "HH:mm" nullable
    val location: String,
    val format: String,         // "In-Person" | "Virtual" | "Hybrid"
    @SerializedName("eventType") val eventType: String,    // "free" | "paid"
    val price: Int,
    val capacity: Int,
    @SerializedName("seatingType")     val seatingType: String,      // "open" | "reserved"
    @SerializedName("paymentMethod")   val paymentMethod: String?,
    @SerializedName("accountName")     val accountName: String?,
    @SerializedName("accountNumber")   val accountNumber: String?,
    @SerializedName("virtualPlatform") val virtualPlatform: String?,
    @SerializedName("virtualLink")     val virtualLink: String?,
    @SerializedName("noRefundPolicy")  val noRefundPolicy: Boolean,
    @SerializedName("isDraft")         val isDraft: Boolean
)

// ── Response from POST /api/events ───────────────────────────────────────────

data class CreateEventResponse(
    val id: Long,
    val title: String,
    val message: String?
)

// ── Internal UI state (not sent directly) ────────────────────────────────────

data class CreateEventFormState(
    // Step 1 — Basics
    val title: String = "",
    val description: String = "",
    val coverImagePath: String? = null,   // local file URI for upload preview

    // Step 2 — Date, Time & Format
    val date: String = "",                // "YYYY-MM-DD"
    val startTime: String = "",           // "HH:mm"
    val endTime: String = "",             // "HH:mm"
    val format: String = "In-Person",     // "In-Person" | "Virtual" | "Hybrid"
    val location: String = "",
    val virtualPlatform: String = "",
    val virtualLink: String = "",

    // Step 3 — Capacity & Seating
    val capacity: String = "",
    val seatingType: String = "open",     // "open" | "reserved"

    // Step 4 — Pricing & Payment
    val eventType: String = "free",       // "free" | "paid"
    val price: String = "",
    val paymentMethod: String = "",       // "GCash" | "Maya" | "Bank"
    val accountName: String = "",
    val accountNumber: String = "",
    val noRefundPolicy: Boolean = false
)
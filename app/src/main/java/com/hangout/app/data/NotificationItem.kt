package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class NotificationItem(
    val id: Long,
    val type: String?,
    val title: String?,
    val body: String?,
    @SerializedName("referenceId")   val referenceId: Long?,
    @SerializedName("referenceType") val referenceType: String?,
    @SerializedName("isRead")        val isRead: Boolean = false,
    @SerializedName("createdAt")     val createdAt: String?
)
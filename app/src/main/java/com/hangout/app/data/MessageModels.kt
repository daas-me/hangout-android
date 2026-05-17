package com.hangout.app.data

import com.google.gson.annotations.SerializedName

data class MessageItem(
    val id: Long,
    val senderId: Long,
    val recipientId: Long,
    val content: String,
    @SerializedName("isRead") val isRead: Boolean = false,
    val sentAt: String?
)

data class OtherUser(
    val id: Long,
    val firstname: String?,
    val lastname: String?,
    val email: String?,
    val photo: String?
) {
    fun displayName() = "${firstname ?: ""} ${lastname ?: ""}".trim().ifBlank { email ?: "Unknown" }
    fun initials() = ((firstname?.firstOrNull()?.uppercaseChar()?.toString() ?: "") +
            (lastname?.firstOrNull()?.uppercaseChar()?.toString() ?: "")).ifBlank { "?" }
}

data class ConversationItem(
    val otherUser: OtherUser,
    val lastMessage: MessageItem,
    val unreadCount: Long
)

data class ConversationDetail(
    val messages: List<MessageItem>,
    val otherUser: OtherUser
)
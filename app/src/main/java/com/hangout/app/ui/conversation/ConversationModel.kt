package com.hangout.app.ui.conversation

import android.content.Context
import com.hangout.app.data.ConversationDetail
import com.hangout.app.data.MessageItem
import com.hangout.app.data.OtherUser
import com.hangout.app.repository.MessageRepository
import com.hangout.app.repository.Result

class ConversationModel(context: Context) {
    private val repo = MessageRepository(context)

    suspend fun getConversation(userId: Long, fallbackUser: OtherUser? = null): Result<ConversationDetail> =
        repo.getConversation(userId, fallbackUser)

    suspend fun sendMessage(recipientId: Long, content: String): Result<MessageItem> =
        repo.sendMessage(recipientId, content)
}
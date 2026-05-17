package com.hangout.app.ui.messages

import android.content.Context
import com.hangout.app.data.ConversationItem
import com.hangout.app.repository.MessageRepository
import com.hangout.app.repository.Result

class MessagesModel(context: Context) {
    private val repo = MessageRepository(context)
    suspend fun getConversations(): Result<List<ConversationItem>> = repo.getConversations()
}
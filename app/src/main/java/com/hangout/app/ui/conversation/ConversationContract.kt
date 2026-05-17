package com.hangout.app.ui.conversation

import com.hangout.app.data.ConversationDetail
import com.hangout.app.data.MessageItem

interface ConversationContract {
    interface View {
        fun showConversation(detail: ConversationDetail)
        fun appendMessage(message: MessageItem)
        fun showLoading(show: Boolean)
        fun showSendLoading(show: Boolean)
        fun showError(message: String)
    }
    interface Presenter {
        fun load(userId: Long)
        fun send(recipientId: Long, content: String)
        fun detachView()
    }
}
package com.hangout.app.ui.messages

import com.hangout.app.data.ConversationItem

interface MessagesContract {
    interface View {
        fun showConversations(items: List<ConversationItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showEmpty()
    }
    interface Presenter {
        fun load()
        fun detachView()
    }
}
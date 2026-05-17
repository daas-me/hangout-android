package com.hangout.app.ui.conversation

import com.hangout.app.repository.Result
import com.hangout.app.utils.ChatHolder
import kotlinx.coroutines.*

class ConversationPresenter(
    private var view: ConversationContract.View?,
    private val model: ConversationModel
) : ConversationContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun load(userId: Long) {
        view?.showLoading(true)
        scope.launch {
            val fallback = ChatHolder.currentChatUser
            when (val r = model.getConversation(userId, fallback)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.showConversation(r.data)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(r.message)
                }
            }
        }
    }

    override fun send(recipientId: Long, content: String) {
        if (content.isBlank()) return
        view?.showSendLoading(true)
        scope.launch {
            when (val r = model.sendMessage(recipientId, content)) {
                is Result.Success -> {
                    view?.showSendLoading(false)
                    view?.appendMessage(r.data)
                }
                is Result.Error -> {
                    view?.showSendLoading(false)
                    view?.showError(r.message)
                }
            }
        }
    }

    override fun detachView() { view = null; scope.cancel() }
}
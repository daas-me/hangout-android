package com.hangout.app.ui.messages

import com.hangout.app.repository.Result
import kotlinx.coroutines.*

class MessagesPresenter(
    private var view: MessagesContract.View?,
    private val model: MessagesModel
) : MessagesContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun load() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getConversations()) {
                is Result.Success -> {
                    view?.showLoading(false)
                    if (r.data.isEmpty()) view?.showEmpty()
                    else view?.showConversations(r.data)
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(r.message)
                }
            }
        }
    }

    override fun detachView() { view = null; scope.cancel() }
}
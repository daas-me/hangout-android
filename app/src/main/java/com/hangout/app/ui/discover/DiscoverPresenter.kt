package com.hangout.app.ui.discover

import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DiscoverPresenter(
    private var view: DiscoverContract.View?,
    private val model: DiscoverModel
) : DiscoverContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadEvents(search: String, filter: String) {
        view?.showLoading(true)
        scope.launch {
            when (val result = model.getDiscoverEvents(search, filter)) {
                is Result.Success -> view?.showEvents(result.data)
                is Result.Error   -> view?.showError(result.message)
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
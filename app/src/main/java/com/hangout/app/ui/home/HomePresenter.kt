package com.hangout.app.ui.home

import android.content.Context
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomePresenter(
    private var view: HomeContract.View?,
    private val model: HomeModel
) : HomeContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll() {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getProfile()) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> view?.showError(r.message)
            }
            when (val r = model.getStats()) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            when (val r = model.getHostingEvents()) {
                is Result.Success -> view?.showHostingEvents(r.data)
                is Result.Error   -> view?.showHostingEvents(emptyList())
            }
            when (val r = model.getTodayEvents()) {
                is Result.Success -> view?.showTodayEvents(r.data)
                is Result.Error   -> view?.showTodayEvents(emptyList())
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
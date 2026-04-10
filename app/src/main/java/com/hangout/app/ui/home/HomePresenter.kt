package com.hangout.app.ui.home

import android.content.Context
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HomePresenter(private var view: HomeContract.View?, context: Context) : HomeContract.Presenter {

    private val userRepo = UserRepository(context)
    private val api      = RetrofitClient.getApiService(context)
    private val scope    = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll() {
        view?.showLoading(true)
        scope.launch {
            when (val r = userRepo.getProfile()) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> { }
            }
            when (val r = userRepo.getStats()) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            try {
                val res = api.getHostingEvents()
                view?.showHostingEvents(
                    if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                view?.showHostingEvents(emptyList())
            }
            try {
                val res = api.getTodayEvents()
                view?.showTodayEvents(
                    if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                view?.showTodayEvents(emptyList())
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
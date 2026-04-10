package com.hangout.app.ui.discover

import android.content.Context
import com.hangout.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DiscoverPresenter(private var view: DiscoverContract.View?, context: Context) : DiscoverContract.Presenter {

    private val api   = RetrofitClient.getApiService(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadEvents(search: String, filter: String) {
        view?.showLoading(true)
        scope.launch {
            try {
                val res = api.getDiscoverEvents(search, filter)
                if (res.isSuccessful) view?.showEvents(res.body() ?: emptyList())
                else view?.showError("Failed to load events")
            } catch (e: Exception) {
                view?.showError("Cannot connect to server.")
            }
            view?.showLoading(false)
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
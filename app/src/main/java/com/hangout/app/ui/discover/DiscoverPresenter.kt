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
    
    // Cache: track last loaded search/filter combination
    private var lastSearchQuery: String? = null
    private var lastFilter: String? = null
    private var cachedEvents: List<com.hangout.app.data.EventItem> = emptyList()

    override fun loadEvents(search: String, filter: String) {
        // Check if we already have this combination cached
        if (lastSearchQuery == search && lastFilter == filter) {
            // Return cached events without showing loading
            view?.showEvents(cachedEvents)
            return
        }
        
        // New search/filter combination - fetch from API
        view?.showLoading(true)
        scope.launch {
            when (val result = model.getDiscoverEvents(search, filter)) {
                is Result.Success -> {
                    // Update cache
                    lastSearchQuery = search
                    lastFilter = filter
                    cachedEvents = result.data
                    view?.showEvents(result.data)
                }
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
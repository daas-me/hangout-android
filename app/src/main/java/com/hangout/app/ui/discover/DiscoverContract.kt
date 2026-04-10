package com.hangout.app.ui.discover

import com.hangout.app.models.EventItem

interface DiscoverContract {
    interface View {
        fun showEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
        fun showError(message: String)
    }
    interface Presenter {
        fun loadEvents(search: String = "", filter: String = "")
        fun detachView()
    }
}
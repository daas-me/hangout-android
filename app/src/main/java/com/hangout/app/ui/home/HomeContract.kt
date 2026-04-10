package com.hangout.app.ui.home

import com.hangout.app.models.EventItem
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats

interface HomeContract {
    interface View {
        fun showProfile(profile: UserProfile)
        fun showStats(stats: UserStats)
        fun showHostingEvents(events: List<EventItem>)
        fun showTodayEvents(events: List<EventItem>)
        fun showLoading(show: Boolean)
    }
    interface Presenter {
        fun loadAll()
        fun detachView()
    }
}
package com.hangout.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import com.hangout.app.network.RetrofitClient
import com.hangout.app.models.EventItem
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val userRepo = UserRepository()
    private val api      = RetrofitClient.apiService

    private val _profile        = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _stats          = MutableLiveData<UserStats?>()
    val stats: LiveData<UserStats?> = _stats

    private val _hostingEvents  = MutableLiveData<List<EventItem>>()
    val hostingEvents: LiveData<List<EventItem>> = _hostingEvents

    private val _todayEvents    = MutableLiveData<List<EventItem>>()
    val todayEvents: LiveData<List<EventItem>> = _todayEvents

    private val _loading        = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun loadAll(token: String) {
        _loading.value = true
        viewModelScope.launch {
            // Profile
            when (val r = userRepo.getProfile(token)) {
                is Result.Success -> _profile.value = r.data
                is Result.Error   -> { }
            }
            // Stats
            when (val r = userRepo.getStats(token)) {
                is Result.Success -> _stats.value = r.data
                is Result.Error   -> { }
            }
            // Hosting events
            try {
                val res = api.getHostingEvents("Bearer $token")
                if (res.isSuccessful) _hostingEvents.value = res.body() ?: emptyList()
                else _hostingEvents.value = emptyList()
            } catch (e: Exception) { _hostingEvents.value = emptyList() }

            // Today/Happening Now events
            try {
                val res = api.getTodayEvents("Bearer $token")
                if (res.isSuccessful) _todayEvents.value = res.body() ?: emptyList()
                else _todayEvents.value = emptyList()
            } catch (e: Exception) { _todayEvents.value = emptyList() }

            _loading.value = false
        }
    }
}
package com.hangout.app.ui.discover

import android.content.Context
import com.hangout.app.data.EventItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache

class DiscoverModel(private val context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getDiscoverEvents(search: String, filter: String): Result<List<EventItem>> {
        val key = AppCache.Keys.discoverSearch(search, filter)
        if (search.isBlank() && filter.isBlank()) {
            // Only cache the default browse, not search queries
            AppCache.get(context, AppCache.Keys.DISCOVER_EVENTS)?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<List<EventItem>>() {}.type
                return Result.Success(com.google.gson.Gson().fromJson(json, type))
            }
        }
        return try {
            val response = api.getDiscoverEvents(search, filter)
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                if (search.isBlank() && filter.isBlank()) {
                    AppCache.put(context, AppCache.Keys.DISCOVER_EVENTS,
                        com.google.gson.Gson().toJson(body), AppCache.TTL.EVENTS_LIST)
                }
                Result.Success(body)
            } else Result.Error("Failed to load events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

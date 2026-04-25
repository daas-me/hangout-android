package com.hangout.app.ui.discover

import android.content.Context
import com.hangout.app.models.EventItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result

class DiscoverModel(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getDiscoverEvents(search: String, filter: String): Result<List<EventItem>> {
        return try {
            val response = api.getDiscoverEvents(search, filter)
            if (response.isSuccessful)
                Result.Success(response.body() ?: emptyList())
            else
                Result.Error("Failed to load events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }
}

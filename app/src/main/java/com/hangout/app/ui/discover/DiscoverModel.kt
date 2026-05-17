package com.hangout.app.ui.discover

import android.content.Context
import com.hangout.app.data.EventItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.repository.Result
import com.hangout.app.utils.AppCache
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                // Filter events: only published, non-completed, and future events
                val filteredBody = (response.body() ?: emptyList())
                    .filter { it.isDraft != true && it.status != "completed" && isEventInFuture(it) }
                
                if (search.isBlank() && filter.isBlank()) {
                    AppCache.put(context, AppCache.Keys.DISCOVER_EVENTS,
                        com.google.gson.Gson().toJson(filteredBody), AppCache.TTL.EVENTS_LIST)
                }
                Result.Success(filteredBody)
            } else Result.Error("Failed to load events.")
        } catch (e: Exception) {
            Result.Error("Cannot connect to server.")
        }
    }

    private fun isEventInFuture(event: EventItem): Boolean {
        return try {
            val now = Calendar.getInstance().time
            // Use end time if available, otherwise use start time
            val eventTimeStr = event.endTime ?: event.startTime ?: event.time ?: return false
            val dateStr = event.date ?: return false
            val dateTimeStr = "$dateStr $eventTimeStr"

            val formats = listOf(
                "MMM d, yyyy HH:mm",
                "MMM d, yyyy h:mm a",
                "MMM dd, yyyy HH:mm",
                "MMM dd, yyyy h:mm a",
                "MMMM d, yyyy HH:mm",
                "MMMM d, yyyy h:mm a"
            )

            var eventDateTime: Date? = null
            for (pattern in formats) {
                try {
                    val formatter = SimpleDateFormat(pattern, Locale.US)
                    formatter.isLenient = false
                    eventDateTime = formatter.parse(dateTimeStr)
                    break
                } catch (e: ParseException) {
                    continue
                }
            }

            eventDateTime?.after(now) ?: true
        } catch (e: Exception) {
            true // On any error, include the event
        }
    }
}


package com.hangout.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple TTL-based JSON cache backed by SharedPreferences.
 *
 * Usage:
 *   AppCache.get(ctx, "profile")           // returns cached JSON or null
 *   AppCache.put(ctx, "profile", json, 10) // store for 10 minutes
 *   AppCache.bust(ctx, "profile")          // invalidate one key
 *   AppCache.bustAll(ctx)                  // invalidate everything
 */
object AppCache {

    private const val PREFS_NAME = "hangout_cache"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Read ──────────────────────────────────────────────────────────────

    fun get(ctx: Context, key: String): String? {
        val p       = prefs(ctx)
        val expiry  = p.getLong(expiryKey(key), 0L)
        if (System.currentTimeMillis() > expiry) return null          // expired
        return p.getString(dataKey(key), null)
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * @param ttlMinutes how long to keep this entry (default 5 min)
     */
    fun put(ctx: Context, key: String, json: String, ttlMinutes: Long = 5) {
        val expiry = System.currentTimeMillis() + ttlMinutes * 60_000L
        prefs(ctx).edit()
            .putString(dataKey(key),  json)
            .putLong(expiryKey(key),  expiry)
            .apply()
    }

    // ── Invalidation ──────────────────────────────────────────────────────

    fun bust(ctx: Context, key: String) {
        prefs(ctx).edit()
            .remove(dataKey(key))
            .remove(expiryKey(key))
            .apply()
    }

    fun bustPrefix(ctx: Context, prefix: String) {
        val p    = prefs(ctx)
        val edit = p.edit()
        p.all.keys
            .filter { it.startsWith("data_$prefix") }
            .forEach { dataK ->
                val rawKey = dataK.removePrefix("data_")
                edit.remove(dataKey(rawKey))
                edit.remove(expiryKey(rawKey))
            }
        edit.apply()
    }

    fun bustAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    // ── Key helpers ───────────────────────────────────────────────────────

    private fun dataKey(key: String)   = "data_$key"
    private fun expiryKey(key: String) = "exp_$key"

    // ── Predefined keys (avoids typos across the app) ─────────────────────

    object Keys {
        const val PROFILE          = "user_profile"
        const val STATS            = "user_stats"
        const val PHOTO            = "user_photo"
        const val HOSTING_EVENTS   = "hosting_events"
        const val ATTENDING_EVENTS = "attending_events"
        const val TODAY_EVENTS     = "today_events"
        const val DISCOVER_EVENTS  = "discover_events"
        const val NOTIFICATIONS    = "notifications"
        const val UNREAD_NOTIF     = "unread_notifications"
        const val UNREAD_MESSAGES  = "unread_messages"

        // Per-event detail: use "event_detail_<id>"
        fun eventDetail(id: Long) = "event_detail_$id"
        // Per-event attendees: use "event_attendees_<id>"
        fun eventAttendees(id: Long) = "event_attendees_$id"
        // Per-discover search: use "discover_<query>_<filter>"
        fun discoverSearch(search: String, filter: String) =
            "discover_${search}_${filter}".replace(" ", "_")
    }

    // ── TTL constants (minutes) ───────────────────────────────────────────

    object TTL {
        const val PROFILE         = 10L
        const val EVENTS_LIST     = 3L
        const val EVENT_DETAIL    = 5L
        const val NOTIFICATIONS   = 2L
        const val UNREAD          = 1L
    }
}
package com.hangout.app.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hangout_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN     = "hangout_token"
        private const val KEY_USER_ID   = "hangout_user_id"
        private const val KEY_EMAIL     = "hangout_email"
        private const val KEY_FIRSTNAME = "hangout_firstname"
    }

    fun saveSession(token: String, email: String, firstname: String, userId: Long? = null) {
        val editor = prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FIRSTNAME, firstname)
        
        if (userId != null) {
            editor.putLong(KEY_USER_ID, userId)
        }
        
        editor.apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1)
        return if (id == -1L) null else id
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getFirstname(): String? = prefs.getString(KEY_FIRSTNAME, null)

    fun getBearerToken(): String = "Bearer ${getToken() ?: ""}"

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
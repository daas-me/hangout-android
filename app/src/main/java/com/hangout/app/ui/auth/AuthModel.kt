package com.hangout.app.ui.auth

import android.content.Context
import com.hangout.app.models.LoginResponse
import com.hangout.app.models.MessageResponse
import com.hangout.app.repository.AuthRepository
import com.hangout.app.repository.Result
import com.hangout.app.utils.SessionManager

class AuthModel(context: Context) {

    private val repo    = AuthRepository(context)
    private val session = SessionManager(context)

    fun isLoggedIn(): Boolean = session.isLoggedIn()

    fun saveSession(token: String, email: String, firstname: String) {
        session.saveSession(token, email, firstname)
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return repo.login(email, password)
    }

    suspend fun register(
        firstname: String,
        lastname: String,
        email: String,
        password: String,
        birthdate: String
    ): Result<MessageResponse> {
        return repo.register(firstname, lastname, email, password, birthdate)
    }
}
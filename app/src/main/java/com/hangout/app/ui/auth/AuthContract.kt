package com.hangout.app.ui.auth

interface AuthContract {
    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun onLoginSuccess(token: String, email: String, firstname: String)
        fun onRegisterSuccess()
    }
    interface Presenter {
        fun login(email: String, password: String)
        fun register(
            firstname: String, lastname: String,
            email: String, password: String, birthdate: String
        )
        fun detachView()
    }
}
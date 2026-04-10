package com.hangout.app.ui.auth

import android.content.Context
import com.hangout.app.repository.AuthRepository
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AuthPresenter(private var view: AuthContract.View?, context: Context) : AuthContract.Presenter {

    private val repo  = AuthRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun login(email: String, password: String) {
        view?.showLoading(true)
        scope.launch {
            when (val result = repo.login(email, password)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.onLoginSuccess(
                        result.data.token,
                        result.data.email,
                        result.data.firstname
                    )
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun register(
        firstname: String, lastname: String,
        email: String, password: String, birthdate: String
    ) {
        view?.showLoading(true)
        scope.launch {
            when (val result = repo.register(firstname, lastname, email, password, birthdate)) {
                is Result.Success -> {
                    view?.showLoading(false)
                    view?.onRegisterSuccess()
                }
                is Result.Error -> {
                    view?.showLoading(false)
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
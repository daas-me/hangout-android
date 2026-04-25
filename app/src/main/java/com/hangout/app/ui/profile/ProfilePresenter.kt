package com.hangout.app.ui.profile

import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class ProfilePresenter(
    private var view: ProfileContract.View?,
    private val model: ProfileModel
) : ProfileContract.Presenter {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll(forceRefresh: Boolean) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.getProfile(forceRefresh)) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> view?.showMessage(r.message)
            }
            when (val r = model.getStats(forceRefresh)) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            when (val r = model.getPhoto(forceRefresh)) {
                is Result.Success -> view?.showPhoto(r.data)
                is Result.Error   -> view?.clearPhoto()
            }
            view?.showLoading(false)
        }
    }

    override fun updateProfile(firstname: String, lastname: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.updateProfile(firstname, lastname)) {
                is Result.Success -> {
                    view?.showMessage("Profile updated successfully!")
                    view?.onProfileUpdateSuccess()
                    loadAll(forceRefresh = true)
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun updatePassword(oldPassword: String, newPassword: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.updatePassword(oldPassword, newPassword)) {
                is Result.Success -> {
                    view?.showMessage("Password changed successfully!")
                    view?.onPasswordUpdateSuccess()
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun uploadPhoto(file: File) {
        view?.showLoading(true)
        scope.launch {
            when (val r = model.uploadPhoto(file)) {
                is Result.Success -> {
                    view?.showMessage("Photo updated!")
                    when (val p = model.getPhoto(forceRefresh = true)) {
                        is Result.Success -> view?.showPhoto(p.data)
                        is Result.Error   -> { }
                    }
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun deletePhoto() {
        scope.launch {
            when (val r = model.deletePhoto()) {
                is Result.Success -> {
                    view?.clearPhoto()
                    view?.showMessage("Photo removed.")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
        }
    }

    override fun clearSession() {
        model.clearSession()
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
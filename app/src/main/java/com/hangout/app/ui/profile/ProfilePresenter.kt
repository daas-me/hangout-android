package com.hangout.app.ui.profile

import android.content.Context
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class ProfilePresenter(private var view: ProfileContract.View?, context: Context) : ProfileContract.Presenter {

    private val repo  = UserRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun loadAll() {
        view?.showLoading(true)
        scope.launch {
            when (val r = repo.getProfile()) {
                is Result.Success -> view?.showProfile(r.data)
                is Result.Error   -> view?.showMessage(r.message)
            }
            when (val r = repo.getStats()) {
                is Result.Success -> view?.showStats(r.data)
                is Result.Error   -> { }
            }
            when (val r = repo.getPhoto()) {
                is Result.Success -> view?.showPhoto(r.data)
                is Result.Error   -> view?.clearPhoto()
            }
            view?.showLoading(false)
        }
    }

    override fun updateProfile(firstname: String, lastname: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = repo.updateProfile(firstname, lastname)) {
                is Result.Success -> {
                    view?.showMessage("Profile updated successfully!")
                    view?.onProfileUpdateSuccess()
                }
                is Result.Error -> view?.showMessage(r.message)
            }
            view?.showLoading(false)
        }
    }

    override fun updatePassword(oldPassword: String, newPassword: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = repo.updatePassword(oldPassword, newPassword)) {
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
            when (val r = repo.uploadPhoto(file)) {
                is Result.Success -> {
                    view?.showMessage("Photo updated!")
                    when (val p = repo.getPhoto()) {
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
            when (val r = repo.deletePhoto()) {
                is Result.Success -> {
                    view?.clearPhoto()
                    view?.showMessage("Photo removed.")
                }
                is Result.Error -> view?.showMessage(r.message)
            }
        }
    }

    override fun detachView() {
        view = null
        scope.cancel()
    }
}
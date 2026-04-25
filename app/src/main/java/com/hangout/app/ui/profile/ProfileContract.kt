package com.hangout.app.ui.profile

import com.hangout.app.models.*
import java.io.File

interface ProfileContract {
    interface View {
        fun showProfile(profile: UserProfile)
        fun showStats(stats: UserStats)
        fun showPhoto(photoData: String)
        fun clearPhoto()
        fun showLoading(show: Boolean)
        fun showMessage(message: String)
        fun onProfileUpdateSuccess()
        fun onPasswordUpdateSuccess()
    }
    interface Presenter {
        fun loadAll(forceRefresh: Boolean = false)
        fun updateProfile(firstname: String, lastname: String)
        fun updatePassword(oldPassword: String, newPassword: String)
        fun uploadPhoto(file: File)
        fun deletePhoto()
        fun clearSession()
        fun detachView()
    }
}
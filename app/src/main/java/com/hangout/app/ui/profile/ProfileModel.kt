package com.hangout.app.ui.profile

import android.content.Context
import com.hangout.app.models.MessageResponse
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import com.hangout.app.utils.SessionManager
import java.io.File

class ProfileModel(context: Context) {

    private val repo    = UserRepository(context)
    private val session = SessionManager(context)

    fun clearSession() = session.clearSession()

    suspend fun getProfile(forceRefresh: Boolean = false): Result<UserProfile> =
        repo.getProfile(forceRefresh)

    suspend fun getStats(forceRefresh: Boolean = false): Result<UserStats> =
        repo.getStats(forceRefresh)

    suspend fun getPhoto(forceRefresh: Boolean = false): Result<String> =
        repo.getPhoto(forceRefresh)

    suspend fun updateProfile(firstname: String, lastname: String): Result<MessageResponse> =
        repo.updateProfile(firstname, lastname)

    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<MessageResponse> =
        repo.updatePassword(oldPassword, newPassword)

    suspend fun uploadPhoto(file: File): Result<MessageResponse> =
        repo.uploadPhoto(file)

    suspend fun deletePhoto(): Result<MessageResponse> =
        repo.deletePhoto()
}
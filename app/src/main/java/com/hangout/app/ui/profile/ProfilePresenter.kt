package com.hangout.app.ui.profile

import android.content.Context
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats
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

    // ── In-memory cache ────────────────────────────────────────────────────
    private var cachedProfile: UserProfile? = null
    private var cachedStats: UserStats? = null
    private var cachedPhoto: String? = null
    private var lastFetchTime: Long = 0L

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private fun isCacheValid(): Boolean {
        return cachedProfile != null &&
                (System.currentTimeMillis() - lastFetchTime) < CACHE_TTL_MS
    }

    override fun loadAll(forceRefresh: Boolean) {
        // Serve from cache if still fresh
        if (!forceRefresh && isCacheValid()) {
            cachedProfile?.let { view?.showProfile(it) }
            cachedStats?.let   { view?.showStats(it) }
            cachedPhoto?.let   { view?.showPhoto(it) } ?: view?.clearPhoto()
            return
        }

        view?.showLoading(true)
        scope.launch {
            when (val r = repo.getProfile()) {
                is Result.Success -> { cachedProfile = r.data; view?.showProfile(r.data) }
                is Result.Error   -> view?.showMessage(r.message)
            }
            when (val r = repo.getStats()) {
                is Result.Success -> { cachedStats = r.data; view?.showStats(r.data) }
                is Result.Error   -> { }
            }
            when (val r = repo.getPhoto()) {
                is Result.Success -> { cachedPhoto = r.data; view?.showPhoto(r.data) }
                is Result.Error   -> { cachedPhoto = null; view?.clearPhoto() }
            }
            lastFetchTime = System.currentTimeMillis()
            view?.showLoading(false)
        }
    }

    override fun updateProfile(firstname: String, lastname: String) {
        view?.showLoading(true)
        scope.launch {
            when (val r = repo.updateProfile(firstname, lastname)) {
                is Result.Success -> {
                    cachedProfile = null // bust cache
                    view?.showMessage("Profile updated successfully!")
                    view?.onProfileUpdateSuccess()
                    loadAll(forceRefresh = true) // reload fresh data
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
                        is Result.Success -> { cachedPhoto = p.data; view?.showPhoto(p.data) }
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
                    cachedPhoto = null // bust photo cache
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
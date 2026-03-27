package com.hangout.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hangout.app.models.*
import com.hangout.app.repository.AuthRepository
import com.hangout.app.repository.Result
import com.hangout.app.repository.UserRepository
import kotlinx.coroutines.launch
import java.io.File

// ── Login ─────────────────────────────────────────────────────────────────────

class LoginViewModel : ViewModel() {
    private val repo = AuthRepository()

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> = _loginResult

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            _loginResult.value = repo.login(email, password)
            _loading.value = false
        }
    }
}

// ── Register ──────────────────────────────────────────────────────────────────

class RegisterViewModel : ViewModel() {
    private val repo = AuthRepository()

    private val _registerResult = MutableLiveData<Result<MessageResponse>>()
    val registerResult: LiveData<Result<MessageResponse>> = _registerResult

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun register(firstname: String, lastname: String, email: String, password: String, birthdate: String) {
        _loading.value = true
        viewModelScope.launch {
            _registerResult.value = repo.register(firstname, lastname, email, password, birthdate)
            _loading.value = false
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────

class ProfileViewModel : ViewModel() {
    private val repo = UserRepository()

    private val _profile   = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _stats     = MutableLiveData<UserStats?>()
    val stats: LiveData<UserStats?> = _stats

    private val _photo     = MutableLiveData<String?>()
    val photo: LiveData<String?> = _photo

    private val _loading   = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message   = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _profileUpdateSuccess = MutableLiveData(false)
    val profileUpdateSuccess: LiveData<Boolean> = _profileUpdateSuccess

    private val _passwordUpdateSuccess = MutableLiveData(false)
    val passwordUpdateSuccess: LiveData<Boolean> = _passwordUpdateSuccess

    fun loadAll(token: String) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.getProfile(token)) {
                is Result.Success -> _profile.value = r.data
                is Result.Error   -> _message.value = r.message
            }
            when (val r = repo.getStats(token)) {
                is Result.Success -> _stats.value = r.data
                is Result.Error   -> { /* stats are non-critical */ }
            }
            when (val r = repo.getPhoto(token)) {
                is Result.Success -> _photo.value = r.data
                is Result.Error   -> { /* no photo is fine */ }
            }
            _loading.value = false
        }
    }

    fun updateProfile(token: String, firstname: String, lastname: String) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.updateProfile(token, firstname, lastname)) {
                is Result.Success -> {
                    _profileUpdateSuccess.value = true
                    _message.value = "Profile updated successfully!"
                    _profile.value = _profile.value?.copy(firstname = firstname, lastname = lastname)
                }
                is Result.Error -> _message.value = r.message
            }
            _loading.value = false
        }
    }

    fun updatePassword(token: String, oldPassword: String, newPassword: String) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.updatePassword(token, oldPassword, newPassword)) {
                is Result.Success -> {
                    _passwordUpdateSuccess.value = true
                    _message.value = "Password changed successfully!"
                }
                is Result.Error -> _message.value = r.message
            }
            _loading.value = false
        }
    }

    fun uploadPhoto(token: String, file: File) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.uploadPhoto(token, file)) {
                is Result.Success -> {
                    _message.value = "Photo updated!"
                    // Reload photo
                    when (val p = repo.getPhoto(token)) {
                        is Result.Success -> _photo.value = p.data
                        else -> { }
                    }
                }
                is Result.Error -> _message.value = r.message
            }
            _loading.value = false
        }
    }

    fun deletePhoto(token: String) {
        _loading.value = true
        viewModelScope.launch {
            when (val r = repo.deletePhoto(token)) {
                is Result.Success -> {
                    _photo.value = null
                    _message.value = "Photo removed."
                }
                is Result.Error -> _message.value = r.message
            }
            _loading.value = false
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearProfileUpdateSuccess() { _profileUpdateSuccess.value = false }
    fun clearPasswordUpdateSuccess() { _passwordUpdateSuccess.value = false }
}
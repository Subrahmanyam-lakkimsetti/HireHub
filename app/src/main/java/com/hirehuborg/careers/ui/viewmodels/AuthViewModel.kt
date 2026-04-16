package com.hirehuborg.careers.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.User
import com.hirehuborg.careers.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User? = null, val message: String = "") : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.fold(
                onSuccess = {
                    _authState.value = AuthState.Success(message = "Login successful")
                },
                onFailure = {
                    _authState.value = AuthState.Error(
                        it.message?.toFriendlyError() ?: "Login failed. Please try again."
                    )
                }
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.register(name, email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Success(user = user, message = "Account created!")
                },
                onFailure = {
                    _authState.value = AuthState.Error(
                        it.message?.toFriendlyError() ?: "Registration failed. Please try again."
                    )
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun String.toFriendlyError(): String = when {
        contains("email address is already in use", ignoreCase = true) ->
            "This email is already registered."
        contains("no user record", ignoreCase = true) ||
                contains("user-not-found", ignoreCase = true) ->
            "No account found with this email."
        contains("password is invalid", ignoreCase = true) ||
                contains("wrong-password", ignoreCase = true) ->
            "Incorrect password. Please try again."
        // This is the actual error Firebase throws — caught from your crash log
        contains("auth credential is incorrect", ignoreCase = true) ||
                contains("malformed", ignoreCase = true) ||
                contains("has expired", ignoreCase = true) ||
                contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
            "Invalid credentials. Please try again."
        contains("network", ignoreCase = true) ->
            "Network error. Please check your connection."
        contains("too-many-requests", ignoreCase = true) ->
            "Too many attempts. Please try again later."
        contains("email address is badly formatted", ignoreCase = true) ->
            "Invalid email format."
        else ->
            "Something went wrong. Please try again."
    }
}
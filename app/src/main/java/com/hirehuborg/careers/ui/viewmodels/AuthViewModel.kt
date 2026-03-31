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
                        it.message?.toFriendlyError() ?: "Login failed"
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
                        it.message?.toFriendlyError() ?: "Registration failed"
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
        contains("email address is already in use") -> "This email is already registered"
        contains("no user record") || contains("user-not-found") -> "No account found with this email"
        contains("password is invalid") || contains("wrong-password") -> "Incorrect password"
        contains("network") || contains("Network") -> "Network error. Check your connection"
        contains("too-many-requests") -> "Too many attempts. Try again later"
        contains("email address is badly formatted") -> "Invalid email format"
        else -> this
    }
}
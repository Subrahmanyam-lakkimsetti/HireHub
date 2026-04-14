package com.hirehuborg.careers.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.data.model.User
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileUpdateState {
    object Idle                        : ProfileUpdateState()
    object Loading                     : ProfileUpdateState()
    data class Success(val msg: String): ProfileUpdateState()
    data class Error(val msg: String)  : ProfileUpdateState()
}

class MainViewModel : ViewModel() {

    private val auth     = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference(Constants.RTDB_USERS_NODE)

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _profileUpdateState =
        MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: LiveData<ProfileUpdateState> = _profileUpdateState

    // ── Load user profile ─────────────────────────────────────────────────────
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = usersRef.child(uid).get().await()
                val user     = snapshot.getValue(User::class.java)
                if (user != null) _user.postValue(user)
                else {
                    // Build from FirebaseAuth if RTDB entry missing
                    _user.postValue(
                        User(
                            uid   = uid,
                            name  = auth.currentUser?.displayName ?: "User",
                            email = auth.currentUser?.email ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Load profile error: ${e.message}")
            }
        }
    }

    // ── Update profile ────────────────────────────────────────────────────────
    fun updateProfile(name: String, email: String) {
        val uid = auth.currentUser?.uid ?: return

        if (name.isBlank()) {
            _profileUpdateState.value = ProfileUpdateState.Error("Name cannot be empty.")
            return
        }

        _profileUpdateState.value = ProfileUpdateState.Loading
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name"  to name.trim(),
                    "email" to email.trim()
                )
                usersRef.child(uid).updateChildren(updates).await()

                // Refresh local user object
                val updated = _user.value?.copy(
                    name  = name.trim(),
                    email = email.trim()
                )
                if (updated != null) _user.postValue(updated)

                _profileUpdateState.postValue(
                    ProfileUpdateState.Success("Profile updated successfully!")
                )
            } catch (e: Exception) {
                _profileUpdateState.postValue(
                    ProfileUpdateState.Error("Update failed: ${e.message}")
                )
            }
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    fun getCurrentUserEmail(): String =
        auth.currentUser?.email ?: ""

    fun logout() {
        auth.signOut()
    }
}
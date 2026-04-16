package com.hirehuborg.careers.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hirehuborg.careers.data.model.User
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val msg: String) : ProfileUpdateState()
    data class Error(val msg: String) : ProfileUpdateState()
}

class MainViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference(Constants.RTDB_USERS_NODE)

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _profileUpdateState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: LiveData<ProfileUpdateState> = _profileUpdateState

    // Keep reference to remove listener on clear
    private var userListener: ValueEventListener? = null
    private var listenerUid: String? = null

    // ── Load user profile (realtime listener) ────────────────────────────────
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        // Remove any existing listener first
        removeUserListener()

        listenerUid = uid
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    _user.postValue(user)
                } else {
                    _user.postValue(
                        User(
                            uid = uid,
                            name = auth.currentUser?.displayName ?: "User",
                            email = auth.currentUser?.email ?: ""
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Listener cancelled: ${error.message}")
            }
        }

        usersRef.child(uid).addValueEventListener(userListener!!)
    }

    // ── Force refresh (call this after resume upload) ────────────────────────
    fun refreshUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = usersRef.child(uid).get().await()
                val user = snapshot.getValue(User::class.java)
                if (user != null) _user.postValue(user)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Refresh error: ${e.message}")
            }
        }
    }

    // ── Update profile ───────────────────────────────────────────────────────
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
                    "name" to name.trim(),
                    "email" to email.trim()
                )
                usersRef.child(uid).updateChildren(updates).await()
                // No need to manually update _user — the realtime listener will fire
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

    fun logout() {
        removeUserListener()
        auth.signOut()
    }

    private fun removeUserListener() {
        val uid = listenerUid ?: return
        userListener?.let {
            usersRef.child(uid).removeEventListener(it)
        }
        userListener = null
        listenerUid = null
    }

    override fun onCleared() {
        super.onCleared()
        removeUserListener()
    }
}
package com.hirehuborg.careers.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.data.model.User
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Realtime Database reference
    private val db = FirebaseDatabase.getInstance()
    private val usersRef = db.getReference(Constants.RTDB_USERS_NODE)

    val currentUser: FirebaseUser?
        get() = auth.currentUser


    suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("User creation failed"))

            val user = User(
                uid = firebaseUser.uid,
                name = name.trim(),
                email = email.trim(),
                createdAt = System.currentTimeMillis()
            )

            // Save to Realtime Database: /users/{uid}
            usersRef.child(firebaseUser.uid).setValue(user.toMap()).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Login failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val snapshot = usersRef.child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("User profile not found"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun logout() {
        auth.signOut()
    }
}
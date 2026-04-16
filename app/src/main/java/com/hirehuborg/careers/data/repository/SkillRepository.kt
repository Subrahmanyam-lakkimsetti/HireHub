package com.hirehuborg.careers.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.tasks.await

class SkillRepository {

    private val auth      = FirebaseAuth.getInstance()
    private val database  = FirebaseDatabase.getInstance()
    private val resumesRef = database.getReference(Constants.RTDB_RESUMES_NODE)
    private val usersRef   = database.getReference(Constants.RTDB_USERS_NODE)

    /**
     * Saves detected skills list to two RTDB locations:
     *  - /resumes/{uid}/detectedSkills   → used by ResumeAnalysis + Gemini
     *  - /users/{uid}/skills             → used for job matching in Module 8
     */
    suspend fun saveDetectedSkills(skills: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not logged in."))

        return try {
            resumesRef.child(uid).child("detectedSkills").setValue(skills).await()
            usersRef.child(uid).child("skills").setValue(skills).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not save skills: ${e.message}"))
        }
    }

    /**
     * Reads saved skills from /resumes/{uid}/detectedSkills
     */
    suspend fun getSavedSkills(): Result<List<String>> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not logged in."))

        return try {
            val snapshot = resumesRef.child(uid).child("detectedSkills").get().await()
            @Suppress("UNCHECKED_CAST")
            val skills = snapshot.value as? List<String> ?: emptyList()
            Result.success(skills)
        } catch (e: Exception) {
            Result.failure(Exception("Could not load skills: ${e.message}"))
        }
    }
}
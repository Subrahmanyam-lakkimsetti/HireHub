package com.hirehuborg.careers.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.utils.Constants
import com.hirehuborg.careers.utils.JobMatcher
import kotlinx.coroutines.tasks.await

class RecommendationRepository {

    private val auth      = FirebaseAuth.getInstance()
    private val database  = FirebaseDatabase.getInstance()
    private val usersRef  = database.getReference(Constants.RTDB_USERS_NODE)

    /**
     * Loads user's skills from RTDB then runs the matching engine.
     *
     * @param jobs Raw job list fetched from APIs
     * @return Result containing:
     *   - List<JobMatch> sorted by score if skills exist
     *   - Empty list (not failure) if no skills saved yet
     */
    suspend fun getMatchedJobs(jobs: List<Job>): Result<MatchResult> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not logged in."))

        return try {
            // Read skills from /users/{uid}/skills
            val snapshot = usersRef.child(uid).child("skills").get().await()

            @Suppress("UNCHECKED_CAST")
            val skills = snapshot.value as? List<String> ?: emptyList()

            if (skills.isEmpty()) {
                // User hasn't uploaded a resume yet — return all jobs unfiltered
                return Result.success(
                    MatchResult(
                        matches       = emptyList(),
                        userSkills    = emptyList(),
                        hasSkills     = false
                    )
                )
            }

            // Run matching engine
            val matches = JobMatcher.match(jobs, skills)

            Result.success(
                MatchResult(
                    matches    = matches,
                    userSkills = skills,
                    hasSkills  = true
                )
            )

        } catch (e: Exception) {
            Result.failure(Exception("Could not load recommendations: ${e.message}"))
        }
    }

    data class MatchResult(
        val matches: List<JobMatch>,
        val userSkills: List<String>,
        val hasSkills: Boolean
    )
}
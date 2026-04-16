package com.hirehuborg.careers.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedJobsRepository {

    private val auth        = FirebaseAuth.getInstance()
    private val database    = FirebaseDatabase.getInstance()

    private val savedJobsRef get() = database
        .getReference(Constants.RTDB_SAVED_JOBS_NODE)
        .child(auth.currentUser?.uid ?: "anonymous")

    // ── Save a job ────────────────────────────────────────────────────────────

    /**
     * Saves a job to RTDB: /saved_jobs/{uid}/{jobId}
     * Uses a flattened map — no nested objects, RTDB-friendly.
     */
    suspend fun saveJob(job: Job): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not logged in."))

            val jobMap = mapOf(
                "id"          to job.id,
                "title"       to job.title,
                "company"     to job.company,
                "location"    to job.location,
                "salary"      to job.salary,
                "description" to job.description,
                "applyUrl"    to job.applyUrl,
                "postedAt"    to job.postedAt,
                "source"      to job.source,
                "tags"        to job.tags,
                "savedAt"     to System.currentTimeMillis()
            )

            savedJobsRef.child(job.id).setValue(jobMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not save job: ${e.message}"))
        }
    }

    // ── Remove a saved job ────────────────────────────────────────────────────

    suspend fun removeJob(jobId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(Exception("Not logged in."))
            savedJobsRef.child(jobId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Could not remove job: ${e.message}"))
        }
    }

    // ── Check if a job is saved ───────────────────────────────────────────────

    suspend fun isJobSaved(jobId: String): Boolean {
        return try {
            auth.currentUser?.uid ?: return false
            val snapshot = savedJobsRef.child(jobId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    // ── Observe saved jobs in real time (Flow) ────────────────────────────────

    /**
     * Returns a Flow that emits the full saved jobs list
     * every time RTDB data changes — real-time updates.
     */
    fun observeSavedJobs(): Flow<List<Job>> = callbackFlow {
        auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jobs = mutableListOf<Job>()
                for (child in snapshot.children) {
                    try {
                        val job = Job(
                            id          = child.child("id").getValue(String::class.java) ?: "",
                            title       = child.child("title").getValue(String::class.java) ?: "",
                            company     = child.child("company").getValue(String::class.java) ?: "",
                            location    = child.child("location").getValue(String::class.java) ?: "",
                            salary      = child.child("salary").getValue(String::class.java) ?: "",
                            description = child.child("description").getValue(String::class.java) ?: "",
                            applyUrl    = child.child("applyUrl").getValue(String::class.java) ?: "",
                            postedAt    = child.child("postedAt").getValue(String::class.java) ?: "",
                            source      = child.child("source").getValue(String::class.java) ?: "",
                            tags        = child.child("tags").children
                                .mapNotNull { it.getValue(String::class.java) },
                            isSaved     = true
                        )
                        if (job.id.isNotBlank()) jobs.add(job)
                    } catch (e: Exception) {
                        Log.e("SavedJobsRepo", "Parse error: ${e.message}")
                    }
                }
                trySend(jobs)
            }

            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }

        savedJobsRef.addValueEventListener(listener)

        // Clean up listener when Flow is cancelled
        awaitClose {
            savedJobsRef.removeEventListener(listener)
        }
    }

    // ── Fetch saved job IDs (for marking isSaved on job cards) ───────────────

    suspend fun getSavedJobIds(): Set<String> {
        return try {
            auth.currentUser?.uid ?: return emptySet()
            val snapshot = savedJobsRef.get().await()
            snapshot.children.mapNotNull { it.key }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
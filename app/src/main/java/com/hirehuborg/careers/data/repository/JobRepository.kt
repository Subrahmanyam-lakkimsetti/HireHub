package com.hirehuborg.careers.data.repository

import android.util.Log
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class JobRepository {

    private val remotiveApi   = RetrofitClient.remotiveApi
    private val arbeitnowApi  = RetrofitClient.arbeitnowApi

    /**
     * Fetches jobs from both APIs in parallel using coroutineScope + async.
     * Merges → deduplicates → filters → sorts by newest first.
     *
     * @param searchQuery optional keyword filter applied after merge
     * @return Result<List<Job>>
     */
    suspend fun fetchAllJobs(searchQuery: String = ""): Result<List<Job>> =
        coroutineScope {
            try {
                // ── Parallel API calls ────────────────────────────────────────
                val remotiveDeferred   = async { fetchRemotiveJobs() }
                val arbeitnowDeferred  = async { fetchArbeitnowJobs() }

                val remotiveJobs  = remotiveDeferred.await()
                val arbeitnowJobs = arbeitnowDeferred.await()

                // ── Merge ─────────────────────────────────────────────────────
                val allJobs = mutableListOf<Job>()
                allJobs.addAll(remotiveJobs)
                allJobs.addAll(arbeitnowJobs)

                // ── Deduplicate by title + company ────────────────────────────
                val seen = mutableSetOf<String>()
                val unique = allJobs.filter { job ->
                    val key = "${job.title.lowercase().trim()}_${job.company.lowercase().trim()}"
                    seen.add(key)
                }

                // ── Filter by search query ────────────────────────────────────
                val filtered = if (searchQuery.isBlank()) {
                    unique
                } else {
                    val q = searchQuery.lowercase()
                    unique.filter { job ->
                        job.title.lowercase().contains(q) ||
                                job.company.lowercase().contains(q) ||
                                job.tags.any { it.lowercase().contains(q) } ||
                                job.location.lowercase().contains(q)
                    }
                }

                // ── Sort: Remotive jobs have ISO dates, Arbeitnow has epoch ──
                val sorted = filtered.sortedByDescending { job ->
                    job.postedAt.toLongOrNull() ?: 0L
                }

                if (sorted.isEmpty() && searchQuery.isNotBlank()) {
                    return@coroutineScope Result.failure(
                        Exception("No jobs found for \"$searchQuery\". Try a different keyword.")
                    )
                }

                Result.success(sorted)

            } catch (e: Exception) {
                Result.failure(Exception("Failed to load jobs: ${e.message}"))
            }
        }

    // ── Private fetchers ──────────────────────────────────────────────────────

    private suspend fun fetchRemotiveJobs(): List<Job> {
        return try {
            val response = remotiveApi.getJobs(limit = 40)
            if (response.isSuccessful) {
                response.body()?.jobs?.map { it.toJob() } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("JobRepository", "Remotive failed: ${e.message}")
            emptyList() // don't fail entire merge if one API is down
        }
    }

    private suspend fun fetchArbeitnowJobs(): List<Job> {
        return try {
            val response = arbeitnowApi.getJobs(page = 1)
            if (response.isSuccessful) {
                response.body()?.data?.map { it.toJob() } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("JobRepository", "Arbeitnow failed: ${e.message}")
            emptyList()
        }
    }
}
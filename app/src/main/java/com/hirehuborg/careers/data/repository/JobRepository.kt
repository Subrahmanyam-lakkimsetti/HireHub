package com.hirehuborg.careers.data.repository

import android.util.Log
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.network.ApiKeys
import com.hirehuborg.careers.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * CSE / software-domain keywords used to filter and also as default search terms.
 * Jobs must contain at least one of these to be shown.
 */
private val CSE_KEYWORDS = listOf(
    "software", "developer", "engineer", "programmer", "coder",
    "android", "ios", "mobile", "web", "frontend", "backend", "fullstack", "full stack",
    "data science", "machine learning", "ai", "artificial intelligence", "ml",
    "devops", "cloud", "aws", "azure", "gcp", "sre",
    "cybersecurity", "security", "network", "system",
    "java", "kotlin", "python", "javascript", "typescript", "react", "node",
    "flutter", "swift", "c++", "golang", "rust",
    "database", "sql", "nosql", "mongodb",
    "testing", "qa", "quality assurance",
    "it ", "information technology", "computer science", "cse", "btech", "b.tech"
)

class JobRepository {

    private val adzunaApi  = RetrofitClient.adzunaApi
    private val jSearchApi = RetrofitClient.jSearchApi

    /**
     * Fetches CSE/tech jobs from India in parallel from Adzuna + JSearch.
     * Merges → deduplicates → CSE-filters → sorts newest first.
     *
     * @param searchQuery optional keyword filter (applied on top of CSE filter)
     * @return Result<List<Job>>
     */
    suspend fun fetchAllJobs(searchQuery: String = ""): Result<List<Job>> =
        coroutineScope {
            try {
                // ── Build effective query (resume keyword or default CSE term) ──
                val effectiveQuery = if (searchQuery.isNotBlank()) searchQuery
                else "software developer"

                // ── Parallel API calls ────────────────────────────────────────
                val adzunaDeferred  = async { fetchAdzunaJobs(effectiveQuery) }
                val jSearchDeferred = async { fetchJSearchJobs(effectiveQuery) }

                val adzunaJobs  = adzunaDeferred.await()
                val jSearchJobs = jSearchDeferred.await()

                // ── Merge ─────────────────────────────────────────────────────
                val allJobs = adzunaJobs + jSearchJobs

                // ── Deduplicate by normalised title + company ─────────────────
                val seen = mutableSetOf<String>()
                val unique = allJobs.filter { job ->
                    val key = "${job.title.normalise()}_${job.company.normalise()}"
                    seen.add(key)   // add() returns false if already present
                }

                // ── CSE domain filter (keep only tech/CS roles) ───────────────
                val cseOnly = unique.filter { job -> isCseJob(job) }

                // ── Optional user search filter on top ────────────────────────
                val filtered = if (searchQuery.isBlank()) {
                    cseOnly
                } else {
                    val q = searchQuery.lowercase()
                    cseOnly.filter { job ->
                        job.title.lowercase().contains(q) ||
                                job.company.lowercase().contains(q) ||
                                job.tags.any { it.lowercase().contains(q) } ||
                                job.location.lowercase().contains(q) ||
                                job.description.lowercase().contains(q)
                    }
                }

                // ── Sort: ISO dates (Adzuna) and epoch millis (JSearch) ───────
                val sorted = filtered.sortedByDescending { job ->
                    // Try epoch first (JSearch), then parse ISO (Adzuna)
                    job.postedAt.toLongOrNull()
                        ?: runCatching {
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                java.util.Locale.US
                            ).parse(job.postedAt)?.time
                        }.getOrNull()
                        ?: 0L
                }

                if (sorted.isEmpty() && searchQuery.isNotBlank()) {
                    return@coroutineScope Result.failure(
                        Exception("No CSE jobs found for \"$searchQuery\". Try a broader keyword.")
                    )
                }

                Result.success(sorted)

            } catch (e: Exception) {
                Log.e("JobRepository", "fetchAllJobs failed: ${e.message}", e)
                Result.failure(Exception("Failed to load jobs: ${e.message}"))
            }
        }

    // ── Private fetchers ──────────────────────────────────────────────────────

    private suspend fun fetchAdzunaJobs(query: String): List<Job> {
        return try {
            val response = adzunaApi.getJobs(
                page           = 1,
                appId          = ApiKeys.ADZUNA_APP_ID,
                appKey         = ApiKeys.ADZUNA_APP_KEY,
                query          = query,
                resultsPerPage = 40,
                sortBy         = "date",
                maxDaysOld     = 30
            )
            if (response.isSuccessful) {
                response.body()?.results?.map { it.toJob() } ?: emptyList()
            } else {
                Log.w("JobRepository", "Adzuna HTTP ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Adzuna failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchJSearchJobs(query: String): List<Job> {
        return try {
            val response = jSearchApi.getJobs(
                apiKey     = ApiKeys.JSEARCH_KEY,
                query      = "$query India",   // always scoped to India
                page       = 1,
                datePosted = "month",
                country    = "in"
            )
            if (response.isSuccessful) {
                response.body()?.data?.map { it.toJob() } ?: emptyList()
            } else {
                Log.w("JobRepository", "JSearch HTTP ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "JSearch failed: ${e.message}")
            emptyList()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun String.normalise() = lowercase().trim()

    /**
     * Returns true if the job belongs to the CSE/tech domain.
     * Checks title, tags, and a brief slice of description.
     */
    private fun isCseJob(job: Job): Boolean {
        val haystack = buildString {
            append(job.title.lowercase())
            append(" ")
            append(job.tags.joinToString(" ").lowercase())
            append(" ")
            append(job.description.take(300).lowercase())
        }
        return CSE_KEYWORDS.any { keyword -> haystack.contains(keyword) }
    }
}
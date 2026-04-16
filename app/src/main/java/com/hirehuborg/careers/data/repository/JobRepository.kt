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

    // 💻 Core Roles
    "software engineer", "software developer", "application developer",
    "programmer", "coder", "sde", "sde1", "sde2",
    "backend developer", "frontend developer", "full stack developer", "fullstack developer",

    // 🌐 Web & App Development
    "web developer", "web development", "mobile developer", "mobile app developer",
    "android developer", "ios developer",

    // ⚛️ Frontend
    "frontend", "front end", "ui developer", "client side",
    "react", "react.js", "reactjs",
    "html", "html5", "css", "css3", "javascript", "js", "typescript", "ts",
    "redux", "tailwind", "bootstrap", "material ui",

    // 🧠 Backend
    "backend", "back end", "server side",
    "node", "nodejs", "node js", "node.js",
    "express", "expressjs", "express js",
    "rest api", "restful api", "api development",
    "mvc", "microservices",

    // 🗄️ Database
    "database", "dbms", "sql", "mysql", "postgresql",
    "nosql", "mongodb", "mongo", "mongoose",
    "database design", "query optimization",

    // ☁️ Cloud & DevOps
    "cloud", "cloud computing", "aws", "amazon web services",
    "azure", "gcp", "google cloud",
    "devops", "ci cd", "docker", "kubernetes", "jenkins",
    "sre", "site reliability engineer",

    // 🤖 AI / Data
    "data science", "data analyst", "machine learning", "ml",
    "artificial intelligence", "ai", "deep learning",
    "nlp", "computer vision",

    // 🔐 Security
    "cybersecurity", "information security", "network security",
    "penetration testing", "ethical hacking",

    // ⚙️ Programming Languages
    "java", "kotlin", "python", "c++", "c", "golang", "go", "rust",
    "swift", "dart",

    // 📱 Mobile / Cross-platform
    "android", "android development",
    "flutter", "react native", "cross platform",

    // 🧪 Testing
    "testing", "qa", "quality assurance",
    "unit testing", "integration testing", "automation testing",

    // 📚 Core CS
    "data structures", "algorithms", "dsa",
    "operating systems", "os",
    "computer networks", "cn",

    // 🎓 Education / Degree Keywords
    "information technology", "computer science", "cse",
    "btech", "b.tech", "bachelor of technology"
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
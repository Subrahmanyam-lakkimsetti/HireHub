package com.hirehuborg.careers.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hirehuborg.careers.data.model.ResumeAnalysis
import com.hirehuborg.careers.network.GeminiClient
import com.hirehuborg.careers.utils.Constants
import com.hirehuborg.careers.utils.GeminiPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AnalysisRepository {

    private val auth       = FirebaseAuth.getInstance()
    private val database   = FirebaseDatabase.getInstance()
    private val resumesRef = database.getReference(Constants.RTDB_RESUMES_NODE)

    /**
     * Sends resume text + skills to Gemini and returns parsed ResumeAnalysis.
     * Also persists result to RTDB.
     */
    suspend fun analyzeResume(
        resumeText: String,
        detectedSkills: List<String>
    ): Result<ResumeAnalysis> = withContext(Dispatchers.IO) {

        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(Exception("Not logged in."))

        try {
            // ── Build prompt ────────────────────────────────────────────────
            val prompt = GeminiPromptBuilder.buildResumeAnalysisPrompt(
                resumeText     = resumeText,
                detectedSkills = detectedSkills
            )

            // ── Call Gemini ─────────────────────────────────────────────────
            android.util.Log.d("HireHub", "STEP 1: Before Gemini call")

            val response = GeminiClient.model.generateContent(prompt)

            android.util.Log.d("HireHub", "STEP 2: Gemini response received")

            val rawResponse = response.text

            android.util.Log.d("HireHub", "STEP 3: Raw response = $rawResponse")

            if (rawResponse.isNullOrBlank()) {
                android.util.Log.e("HireHub", "Raw response is EMPTY")
                return@withContext Result.failure(
                    Exception("Gemini returned no response.")
                )
            }

            val stopReason = response.candidates
                .firstOrNull()
                ?.finishReason
                ?.name ?: "UNKNOWN"

            android.util.Log.d("HireHub", "Gemini stop reason: $stopReason")
            android.util.Log.d("HireHub", "Raw response length: ${rawResponse.length}")

            android.util.Log.d("HireHub", "STEP 4: Parsing response")
            // ── Parse structured response ───────────────────────────────────
            val analysis = parseGeminiResponse(rawResponse)

            // ── Persist to RTDB: /resumes/{uid}/analysis ────────────────────
            resumesRef.child(uid).child("analysis").setValue(analysis.toMap()).await()

            // ── Also update ATS score on user profile ───────────────────────
            database.getReference(Constants.RTDB_USERS_NODE)
                .child(uid)
                .child("resumeScore")
                .setValue(analysis.atsScore)
                .await()

            Result.success(analysis)

        } catch (e: Exception) {

            android.util.Log.e("HireHub", "🔥 ERROR OCCURRED", e)

            if (e.message?.contains("MAX_TOKENS", ignoreCase = true) == true) {

                android.util.Log.w("HireHub", "⚠️ Partial response due to MAX_TOKENS")

                try {
                    // Try to still get partial response
                    val partialResponse = e.localizedMessage ?: ""

                    android.util.Log.d("HireHub", "Partial response: $partialResponse")

                    val analysis = parseGeminiResponse(partialResponse)

                    return@withContext Result.success(analysis)

                } catch (parseError: Exception) {
                    android.util.Log.e("HireHub", "Parsing failed for partial response", parseError)
                }
            }

            val message = when {

                e.message?.contains("API key", ignoreCase = true) == true ->
                    "Invalid Gemini API key. Check your BuildConfig setup."
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "Gemini API quota exceeded. Try again later."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Check your internet connection."
                else -> "Analysis failed: ${e.message}"
            }
            Result.failure(Exception(message))
        }
    }

    /**
     * Loads a previously saved analysis from RTDB.
     */
    suspend fun getSavedAnalysis(): Result<ResumeAnalysis> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not logged in."))

        return try {
            val snapshot = resumesRef.child(uid).child("analysis").get().await()
            val analysis = snapshot.getValue(ResumeAnalysis::class.java)
                ?: return Result.failure(Exception("No analysis found. Please analyze your resume first."))
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(Exception("Could not load analysis: ${e.message}"))
        }
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    /**
     * Parses Gemini's structured plain-text response into ResumeAnalysis.
     *
     * Expected format (enforced by prompt):
     *   ATS_SCORE: 74
     *   STRENGTHS:
     *   - point
     *   IMPROVEMENTS:
     *   - point
     *   MISSING_SKILLS:
     *   - skill
     *   RECOMMENDED_ROLES:
     *   - role
     */
    private fun parseGeminiResponse(raw: String): ResumeAnalysis {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

        var atsScore         = 0
        val strengths        = mutableListOf<String>()
        val improvements     = mutableListOf<String>()
        val missingSkills    = mutableListOf<String>()
        val recommendedRoles = mutableListOf<String>()

        var currentSection = ""

        for (line in lines) {
            when {
                // ── Score line ───────────────────────────────────────────────
                line.startsWith("ATS_SCORE:", ignoreCase = true) -> {
                    val scoreStr = line.substringAfter(":").trim()
                    atsScore = scoreStr.filter { it.isDigit() }.toIntOrNull()
                        ?.coerceIn(0, 100) ?: 0
                    currentSection = ""
                }

                // ── Section headers ──────────────────────────────────────────
                line.startsWith("STRENGTHS:", ignoreCase = true) ->
                    currentSection = "STRENGTHS"
                line.startsWith("IMPROVEMENTS:", ignoreCase = true) ->
                    currentSection = "IMPROVEMENTS"
                line.startsWith("MISSING_SKILLS:", ignoreCase = true) ->
                    currentSection = "MISSING_SKILLS"
                line.startsWith("RECOMMENDED_ROLES:", ignoreCase = true) ->
                    currentSection = "RECOMMENDED_ROLES"

                // ── Bullet points ─────────────────────────────────────────────
                line.startsWith("-") -> {
                    val content = line.removePrefix("-").trim()

                    if (content.isBlank()) continue

                    when (currentSection) {
                        "STRENGTHS"         -> strengths.add(content)
                        "IMPROVEMENTS"      -> improvements.add(content)
                        "MISSING_SKILLS"    -> missingSkills.add(content)
                        "RECOMMENDED_ROLES" -> recommendedRoles.add(content)
                    }
                }
            }
        }

        // Fallback: if parsing fails, provide safe defaults rather than crashing
        return ResumeAnalysis(
            atsScore         = atsScore,
            strengths        = strengths.ifEmpty {
                listOf("Could not parse strengths from response.")
            },
            improvements     = improvements.ifEmpty {
                listOf("Could not parse improvements from response.")
            },
            missingSkills    = missingSkills.ifEmpty {
                listOf("Could not parse missing skills from response.")
            },
            recommendedRoles = recommendedRoles.ifEmpty {
                listOf("Could not parse recommended roles from response.")
            },
            analyzedAt       = System.currentTimeMillis()
        )
    }
}
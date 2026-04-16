package com.hirehuborg.careers.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hirehuborg.careers.data.model.Roadmap
import com.hirehuborg.careers.data.model.RoadmapStep
import com.hirehuborg.careers.network.GeminiClient
import com.hirehuborg.careers.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RoadmapRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val roadmapsRef get() = database
        .getReference(Constants.RTDB_ROADMAPS_NODE)
        .child(auth.currentUser?.uid ?: "anonymous")

    /**
     * Generate a learning roadmap for a specific skill using Gemini API
     */
    suspend fun generateRoadmap(skillName: String): Result<Roadmap> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not logged in."))

            // Build prompt for Gemini
            val prompt = buildRoadmapPrompt(skillName)

            // Call Gemini API
            val response = GeminiClient.model.generateContent(prompt)
            val rawResponse = response.text

            if (rawResponse.isNullOrBlank()) {
                return Result.failure(Exception("Gemini returned empty response"))
            }

            // Parse the response into a structured Roadmap
            val roadmap = parseRoadmapResponse(rawResponse, skillName)

            // Save to Firebase
            val roadmapId = "${skillName.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}"
            val roadmapWithId = roadmap.copy(id = roadmapId)

            roadmapsRef.child(roadmapId).setValue(roadmapWithId).await()

            Result.success(roadmapWithId)
        } catch (e: Exception) {
            Log.e("RoadmapRepo", "Error generating roadmap", e)
            Result.failure(Exception("Failed to generate roadmap: ${e.message}"))
        }
    }

    /**
     * Get all saved roadmaps for the current user
     */
    suspend fun getSavedRoadmaps(): List<Roadmap> {
        return try {
            val snapshot = roadmapsRef.get().await()
            val roadmaps = mutableListOf<Roadmap>()
            for (child in snapshot.children) {
                val roadmap = child.getValue(Roadmap::class.java)
                if (roadmap != null) {
                    roadmaps.add(roadmap)
                }
            }
            roadmaps.sortedByDescending { it.generatedAt }
        } catch (e: Exception) {
            Log.e("RoadmapRepo", "Error getting roadmaps", e)
            emptyList()
        }
    }

    /**
     * Observe saved roadmaps in real-time
     */
    fun observeSavedRoadmaps(): Flow<List<Roadmap>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roadmaps = mutableListOf<Roadmap>()
                for (child in snapshot.children) {
                    val roadmap = child.getValue(Roadmap::class.java)
                    if (roadmap != null) {
                        roadmaps.add(roadmap)
                    }
                }
                trySend(roadmaps.sortedByDescending { it.generatedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }

        roadmapsRef.addValueEventListener(listener)

        awaitClose {
            roadmapsRef.removeEventListener(listener)
        }
    }

    /**
     * Delete a saved roadmap
     */
    suspend fun deleteRoadmap(roadmapId: String): Result<Unit> {
        return try {
            roadmapsRef.child(roadmapId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete roadmap: ${e.message}"))
        }
    }

    /**
     * Build the prompt for Gemini to generate a structured roadmap
     */
    private fun buildRoadmapPrompt(skillName: String): String {
        return """
            Generate a comprehensive learning roadmap for mastering $skillName.
            
            Format your response EXACTLY as follows:
            
            TITLE: [Engaging title for this roadmap]
            DURATION: [Estimated total duration like "8-10 weeks" or "3 months"]
            
            STEPS:
            1. [Step Title] | [Duration like "Week 1-2"] | [Detailed description of what to learn and do]
            2. [Step Title] | [Duration] | [Description]
            (Continue for 6-8 steps covering beginner to advanced)
            
            RESOURCES:
            - [Resource Title] | [URL or "Self-study"] | [Type: course/documentation/video/article]
            - [Next resource]
            
            Make the roadmap practical, actionable, and include hands-on projects or exercises.
            Focus on modern best practices and industry-relevant tools.
            Provide 6-8 steps that progressively build expertise.
            Include 5-7 quality resources.
        """.trimIndent()
    }

    /**
     * Parse Gemini's response into a structured Roadmap object
     */
    private fun parseRoadmapResponse(response: String, skillName: String): Roadmap {
        val lines = response.lines()

        var title = "Learn $skillName"
        var duration = "8-10 weeks"
        val steps = mutableListOf<RoadmapStep>()
        val resources = mutableListOf<com.hirehuborg.careers.data.model.Resource>()

        var currentSection = ""
        var stepCounter = 1

        for (line in lines) {
            val trimmedLine = line.trim()

            when {
                trimmedLine.startsWith("TITLE:") -> {
                    title = trimmedLine.substringAfter(":").trim()
                }
                trimmedLine.startsWith("DURATION:") -> {
                    duration = trimmedLine.substringAfter(":").trim()
                }
                trimmedLine.startsWith("STEPS:") -> {
                    currentSection = "STEPS"
                }
                trimmedLine.startsWith("RESOURCES:") -> {
                    currentSection = "RESOURCES"
                }
                currentSection == "STEPS" && trimmedLine.matches(Regex("^\\d+\\..*")) -> {
                    // Parse step line: "1. Step Title | Duration | Description"
                    val parts = trimmedLine.substringAfter(".").trim().split("|")
                    if (parts.size >= 3) {
                        val stepTitle = parts[0].trim()
                        val stepDuration = parts[1].trim()
                        val stepDescription = parts[2].trim()

                        steps.add(
                            RoadmapStep(
                                stepNumber = stepCounter++,
                                title = stepTitle,
                                description = stepDescription,
                                duration = stepDuration
                            )
                        )
                    }
                }
                currentSection == "RESOURCES" && trimmedLine.startsWith("-") -> {
                    // Parse resource line: "- Title | URL | Type"
                    val resourceText = trimmedLine.substringAfter("-").trim()
                    val parts = resourceText.split("|")
                    if (parts.size >= 2) {
                        val resourceTitle = parts[0].trim()
                        val resourceUrl = if (parts.size > 1) parts[1].trim() else "https://www.google.com/search?q=${resourceTitle.replace(" ", "+")}"
                        val resourceType = if (parts.size > 2) parts[2].trim() else "article"

                        resources.add(
                            com.hirehuborg.careers.data.model.Resource(
                                title = resourceTitle,
                                url = resourceUrl,
                                type = resourceType
                            )
                        )
                    }
                }
            }
        }

        // If parsing failed to get steps, create fallback steps
        if (steps.isEmpty()) {
            steps.addAll(getFallbackSteps(skillName))
        }

        // Build the complete roadmap content as markdown-style text
        val roadmapContent = buildString {
            appendLine("# $title")
            appendLine("\n## Estimated Duration: $duration")
            appendLine("\n## Learning Path")
            steps.forEach { step ->
                appendLine("\n### Step ${step.stepNumber}: ${step.title}")
                appendLine("**Duration:** ${step.duration}")
                appendLine("**What to learn:** ${step.description}")
            }
            if (resources.isNotEmpty()) {
                appendLine("\n## Recommended Resources")
                resources.forEach { resource ->
                    appendLine("\n- **${resource.title}** (${resource.type})")
                    appendLine("  Link: ${resource.url}")
                }
            }
        }

        return Roadmap(
            skillName = skillName,
            roadmapContent = roadmapContent,
            steps = steps,
            resources = resources,
            estimatedDuration = duration,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun getFallbackSteps(skillName: String): List<RoadmapStep> {
        return listOf(
            RoadmapStep(1, "Fundamentals of $skillName", "Learn core concepts, syntax, and basic usage", "Week 1-2", emptyList()),
            RoadmapStep(2, "Intermediate $skillName", "Dive deeper into advanced features and patterns", "Week 3-4", emptyList()),
            RoadmapStep(3, "Practical Projects", "Build real-world applications using $skillName", "Week 5-6", emptyList()),
            RoadmapStep(4, "Best Practices", "Learn industry standards and optimization techniques", "Week 7-8", emptyList()),
            RoadmapStep(5, "Advanced Topics", "Master complex scenarios and integrations", "Week 9-10", emptyList())
        )
    }
}
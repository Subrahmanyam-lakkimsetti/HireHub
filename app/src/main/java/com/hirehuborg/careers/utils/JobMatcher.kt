package com.hirehuborg.careers.utils

import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.data.model.JobMatch

/**
 * On-device job ↔ resume skill matching engine.
 *
 * Scoring algorithm:
 *
 * For each job, we check:
 *  1. Job TITLE       — if a skill appears in title → high weight (3 pts)
 *  2. Job TAGS        — if a skill matches a tag    → medium weight (2 pts)
 *  3. Job DESCRIPTION — if a skill appears in desc  → low weight (1 pt)
 *
 * Final score = (raw points / max possible points) * 100, capped at 100.
 * Jobs with score 0 (zero matches anywhere) are excluded entirely.
 */
object JobMatcher {

    /**
     * @param jobs         Full list of jobs fetched from APIs
     * @param userSkills   Detected skills from user's resume (from RTDB)
     * @return             Filtered + scored + sorted list of JobMatch
     */
    fun match(
        jobs: List<Job>,
        userSkills: List<String>
    ): List<JobMatch> {
        if (userSkills.isEmpty()) return emptyList()

        // Normalize skills once — avoid repeated lowercasing inside the loop
        val normalizedSkills = userSkills.map { it.lowercase().trim() }

        val results = jobs.mapNotNull { job ->
            scoreJob(job, normalizedSkills)
        }

        // Sort: highest match first, then alphabetical by title for ties
        return results.sortedWith(
            compareByDescending<JobMatch> { it.matchScore }
                .thenBy { it.job.title }
        )
    }

    /**
     * Scores a single job against the user's skill list.
     * Returns null if zero matches (job will be excluded from results).
     */
    private fun scoreJob(job: Job, normalizedSkills: List<String>): JobMatch? {
        val titleLower = job.title.lowercase()
        val tagsLower  = job.tags.map { it.lowercase() }
        val descLower  = job.description.lowercase().take(2000) // cap desc length

        var rawPoints    = 0
        val matchedSkills = mutableSetOf<String>()

        for (skill in normalizedSkills) {
            var skillMatched = false

            // ── Title match → 3 points ───────────────────────────────────────
            if (containsSkillToken(titleLower, skill)) {
                rawPoints += 3
                matchedSkills.add(skill)
                skillMatched = true
            }

            // ── Tag match → 2 points ─────────────────────────────────────────
            if (tagsLower.any { tag -> containsSkillToken(tag, skill) }) {
                rawPoints += 2
                matchedSkills.add(skill)
                skillMatched = true
            }

            // ── Description match → 1 point ──────────────────────────────────
            if (!skillMatched && containsSkillToken(descLower, skill)) {
                rawPoints += 1
                matchedSkills.add(skill)
            }
        }

        // Exclude jobs with zero matches
        if (rawPoints == 0) return null

        // Max possible = if every skill matched title (3 pts each)
        val maxPoints   = normalizedSkills.size * 3
        val scorePercent = ((rawPoints.toDouble() / maxPoints) * 100)
            .toInt()
            .coerceIn(1, 100)

        return JobMatch(
            job                = job,
            matchScore         = scorePercent,
            matchedSkills      = matchedSkills.toList(),
            totalSkillsMatched = matchedSkills.size
        )
    }

    /**
     * Smart token matching — avoids false positives like
     * "java" matching inside "javascript"
     */
    private fun containsSkillToken(text: String, skill: String): Boolean {
        if (skill.length <= 2) {
            // Short skills need word boundary — e.g. "go", "r", "c#"
            val regex = Regex("\\b${Regex.escape(skill)}\\b")
            return regex.containsMatchIn(text)
        }
        return text.contains(skill)
    }
}
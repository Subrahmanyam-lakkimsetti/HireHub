package com.hirehuborg.careers.data.model



/**
 * Wraps a Job with its computed match score and matched skill list.
 * This is what the adapter and UI work with — never raw Job directly.
 */
data class JobMatch(
    val job: Job,
    val matchScore: Int,              // 0–100
    val matchedSkills: List<String>,  // skills from resume found in this job
    val totalSkillsMatched: Int       // count for badge display
)
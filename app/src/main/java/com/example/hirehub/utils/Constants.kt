package com.example.hirehub.utils

object Constants {

    // Realtime Database
// Realtime Database Nodes
    const val RTDB_USERS_NODE = "users"
    const val RTDB_SAVED_JOBS_NODE = "saved_jobs"
    const val COLLECTION_RESUMES = "resumes"

    // Add to existing Constants.kt
    const val RTDB_RESUMES_NODE = "resumes"
    const val MAX_PDF_SIZE_MB = 5.0

    // Firebase Storage
    const val STORAGE_RESUMES_PATH = "resumes/"

    // Job APIs
    const val REMOTIVE_BASE_URL = "https://remotive.com/api/"
    const val ARBEITNOW_BASE_URL = "https://www.arbeitnow.com/api/"
    const val ADZUNA_BASE_URL = "https://api.adzuna.com/v1/api/"

    // Gemini
    const val GEMINI_MODEL = "gemini-1.5-flash"

    // Intent Keys
    const val EXTRA_RESUME_TEXT = "extra_resume_text"
    const val EXTRA_JOB_ID = "extra_job_id"
    const val EXTRA_JOB_OBJECT = "extra_job_object"

    // SharedPrefs
    const val PREFS_NAME = "hirehub_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_EMAIL = "user_email"

    const val MAX_EXTRACTED_CHARS = 12_000

    // Skill Keywords (used in Module 4)
    val SKILL_KEYWORDS = listOf(
        "kotlin", "java", "python", "javascript", "typescript",
        "react", "angular", "vue", "node.js", "nodejs", "express",
        "spring", "spring boot", "django", "flask", "fastapi",
        "android", "ios", "swift", "flutter", "dart",
        "sql", "mysql", "postgresql", "mongodb", "firebase",
        "aws", "azure", "gcp", "docker", "kubernetes",
        "git", "github", "ci/cd", "jenkins", "linux",
        "machine learning", "deep learning", "tensorflow", "pytorch",
        "html", "css", "rest api", "graphql", "microservices"
    )
}
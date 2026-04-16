package com.hirehuborg.careers.data.model



data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val resumeUrl: String = "",
    val skills: List<String> = emptyList(),
    val resumeScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Required for Firebase Realtime Database deserialization
    constructor() : this("", "", "", "", emptyList(), 0, 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "resumeUrl" to resumeUrl,
        "skills" to skills,
        "resumeScore" to resumeScore,
        "createdAt" to createdAt
    )
}
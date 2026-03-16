package com.example.hirehub.data.model


data class ResumeData(
    val uid: String = "",
    val fileName: String = "",
    val extractedText: String = "",
    val detectedSkills: List<String> = emptyList(),
    val uploadedAt: Long = System.currentTimeMillis()
) {
    // No-arg constructor required by Firebase Realtime Database
    constructor() : this("", "", "", emptyList(), 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "uid"            to uid,
        "fileName"       to fileName,
        "extractedText"  to extractedText,
        "detectedSkills" to detectedSkills,
        "uploadedAt"     to uploadedAt
    )
}
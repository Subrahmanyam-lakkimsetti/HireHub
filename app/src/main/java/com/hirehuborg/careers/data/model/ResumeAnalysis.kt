package com.hirehuborg.careers.data.model

import com.google.firebase.database.PropertyName

data class ResumeAnalysis(
    @get:PropertyName("atsScore")     @set:PropertyName("atsScore")     var atsScore: Int = 0,
    @get:PropertyName("strengths")    @set:PropertyName("strengths")    var strengths: List<String> = emptyList(),
    @get:PropertyName("improvements") @set:PropertyName("improvements") var improvements: List<String> = emptyList(),
    @get:PropertyName("missingSkills")    @set:PropertyName("missingSkills")    var missingSkills: List<String> = emptyList(),
    @get:PropertyName("recommendedRoles") @set:PropertyName("recommendedRoles") var recommendedRoles: List<String> = emptyList(),
    @get:PropertyName("analyzedAt")   @set:PropertyName("analyzedAt")   var analyzedAt: Long = 0L
) {
    constructor() : this(0, emptyList(), emptyList(), emptyList(), emptyList(), 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "atsScore"         to atsScore,
        "strengths"        to strengths,
        "improvements"     to improvements,
        "missingSkills"    to missingSkills,
        "recommendedRoles" to recommendedRoles,
        "analyzedAt"       to analyzedAt
    )
}
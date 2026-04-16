package com.hirehuborg.careers.data.model


/**
 * Represents a named group of detected skills.
 * Used to render grouped chip sections in the UI.
 */
data class SkillCategory(
    val categoryName: String,
    val emoji: String,
    val skills: List<String>,
    val chipColorRes: Int   // color resource id for chips in this category
)
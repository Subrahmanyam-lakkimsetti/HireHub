package com.hirehuborg.careers.utils

object GeminiPromptBuilder {

    fun buildResumeAnalysisPrompt(
        resumeText: String,
        detectedSkills: List<String>
    ): String {

        // Hard cap resume text at 3000 chars before building prompt
        val cappedText = resumeText.take(3_000)

        val skillsLine = if (detectedSkills.isNotEmpty())
            detectedSkills.take(15).joinToString(", ")
        else
            "Not detected"

        return """
You are an ATS resume analyzer.

IMPORTANT RULES:
- Keep answers SHORT and STRICT
- Maximum 3 bullet points per section
- Each bullet must be 1 short sentence (max 12 words)
- Do NOT explain anything extra
- Follow format EXACTLY

RESUME:
$cappedText

SKILLS FOUND: $skillsLine

REPLY FORMAT (STRICT):

ATS_SCORE: [0-100]

STRENGTHS:
- point
- point
- point

IMPROVEMENTS:
- point
- point
- point

MISSING_SKILLS:
- skill
- skill
- skill

RECOMMENDED_ROLES:
- role
- role
- role
""".trimIndent()
    }
}
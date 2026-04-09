package com.hirehuborg.careers.domain.usecases

import com.hirehuborg.careers.data.model.SkillCategory
import com.hirehuborg.careers.data.repository.SkillRepository
import com.hirehuborg.careers.utils.SkillMatcher

/**
 * Domain use case — orchestrates skill detection + persistence.
 *
 * Clean Architecture boundary:
 *  UI → ViewModel → UseCase → Repository → Firebase
 *                           → SkillMatcher (pure logic, no Android deps)
 */
class DetectSkillsUseCase(
    private val skillRepository: SkillRepository
) {
    data class Result(
        val categories: List<SkillCategory>,
        val flatSkills: List<String>
    )

    /**
     * 1. Run SkillMatcher on provided text
     * 2. Persist flat skill list to RTDB
     * 3. Return categorized + flat results
     */
    suspend fun execute(resumeText: String): kotlin.Result<Result> {
        if (resumeText.isBlank()) {
            return kotlin.Result.failure(Exception("Resume text is empty. Please re-upload your resume."))
        }

        val (categories, flatSkills) = SkillMatcher.detect(resumeText)

        if (flatSkills.isEmpty()) {
            return kotlin.Result.failure(
                Exception(
                    "No recognizable skills were found in your resume. " +
                            "Try using a more detailed, text-based PDF."
                )
            )
        }

        // Persist to RTDB
        val saveResult = skillRepository.saveDetectedSkills(flatSkills)
        if (saveResult.isFailure) {
            return kotlin.Result.failure(saveResult.exceptionOrNull()!!)
        }

        return kotlin.Result.success(Result(categories, flatSkills))
    }
}
package com.hirehuborg.careers.domain.usecases

import com.hirehuborg.careers.data.model.ResumeAnalysis
import com.hirehuborg.careers.data.repository.AnalysisRepository

class AnalyzeResumeUseCase(
    private val repository: AnalysisRepository = AnalysisRepository()
) {
    /**
     * Validates inputs then delegates to repository.
     */
    suspend fun execute(
        resumeText: String,
        detectedSkills: List<String>
    ): Result<ResumeAnalysis> {
        if (resumeText.isBlank()) {
            return Result.failure(
                Exception("Resume text is missing. Please re-upload your resume.")
            )
        }
        return repository.analyzeResume(resumeText, detectedSkills)
    }
}
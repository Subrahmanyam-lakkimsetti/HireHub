package com.example.hirehub.domain.usecases

import com.example.hirehub.data.model.ResumeAnalysis
import com.example.hirehub.data.repository.AnalysisRepository

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
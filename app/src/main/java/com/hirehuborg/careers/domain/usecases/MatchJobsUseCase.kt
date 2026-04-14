package com.hirehuborg.careers.domain.usecases

import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.data.repository.RecommendationRepository

class MatchJobsUseCase(
    private val repository: RecommendationRepository = RecommendationRepository()
) {
    sealed class Result {
        data class Matched(
            val matches: List<JobMatch>,
            val userSkills: List<String>
        ) : Result()

        /**
         * User has no resume skills yet — show upload prompt banner
         */
        object NoSkills : Result()

        /**
         * Skills exist but zero jobs matched — very rare
         */
        object NoMatches : Result()

        data class Error(val message: String) : Result()
    }

    suspend fun execute(jobs: List<Job>): Result {
        if (jobs.isEmpty()) return Result.NoMatches

        return repository.getMatchedJobs(jobs).fold(
            onSuccess = { result ->
                when {
                    !result.hasSkills        -> Result.NoSkills
                    result.matches.isEmpty() -> Result.NoMatches
                    else                     -> Result.Matched(
                        result.matches,
                        result.userSkills
                    )
                }
            },
            onFailure = { error ->
                Result.Error(error.message ?: "Recommendation failed.")
            }
        )
    }
}
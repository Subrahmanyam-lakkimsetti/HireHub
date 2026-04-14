package com.hirehuborg.careers.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.JobMatch
import com.hirehuborg.careers.data.repository.JobRepository
import com.hirehuborg.careers.domain.usecases.MatchJobsUseCase
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────
sealed class JobListState {
    object Idle                                        : JobListState()
    object Loading                                     : JobListState()
    data class Success(
        val matches: List<JobMatch>,
        val userSkills: List<String>
    )                                                  : JobListState()
    object NoSkills                                    : JobListState()
    object NoMatches                                   : JobListState()
    data class Error(val message: String)              : JobListState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class JobViewModel : ViewModel() {

    private val jobRepository    = JobRepository()
    private val matchJobsUseCase = MatchJobsUseCase()

    private val _state = MutableLiveData<JobListState>(JobListState.Idle)
    val state: LiveData<JobListState> = _state

    // Full match list — kept for client-side search without re-fetching
    private var allMatches: List<JobMatch> = emptyList()
    private var cachedSkills: List<String> = emptyList()

    private var searchJob: CoroutineJob? = null

    // ── Load ──────────────────────────────────────────────────────────────────
    fun loadJobs(forceRefresh: Boolean = false) {
        if (_state.value is JobListState.Loading) return
        if (!forceRefresh && allMatches.isNotEmpty()) {
            _state.value = JobListState.Success(allMatches, cachedSkills)
            return
        }

        _state.value = JobListState.Loading

        viewModelScope.launch {
            // Step 1 — fetch all jobs from APIs
            val jobsResult = jobRepository.fetchAllJobs()
            if (jobsResult.isFailure) {
                _state.postValue(
                    JobListState.Error(
                        jobsResult.exceptionOrNull()?.message ?: "Failed to load jobs."
                    )
                )
                return@launch
            }

            val jobs = jobsResult.getOrThrow()

            // Step 2 — match against resume skills
            when (val result = matchJobsUseCase.execute(jobs)) {
                is MatchJobsUseCase.Result.Matched -> {
                    allMatches   = result.matches
                    cachedSkills = result.userSkills
                    _state.postValue(
                        JobListState.Success(result.matches, result.userSkills)
                    )
                }
                is MatchJobsUseCase.Result.NoSkills  ->
                    _state.postValue(JobListState.NoSkills)
                is MatchJobsUseCase.Result.NoMatches ->
                    _state.postValue(JobListState.NoMatches)
                is MatchJobsUseCase.Result.Error     ->
                    _state.postValue(JobListState.Error(result.message))
            }
        }
    }

    // ── Search — filters already-matched list locally ─────────────────────────
    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400L)
            if (query.isBlank()) {
                _state.value = JobListState.Success(allMatches, cachedSkills)
                return@launch
            }
            val q        = query.lowercase()
            val filtered = allMatches.filter { match ->
                match.job.title.lowercase().contains(q)   ||
                        match.job.company.lowercase().contains(q) ||
                        match.matchedSkills.any { it.lowercase().contains(q) } ||
                        match.job.location.lowercase().contains(q)
            }
            _state.value = if (filtered.isEmpty())
                JobListState.Error("No matched jobs for \"$query\".")
            else
                JobListState.Success(filtered, cachedSkills)
        }
    }

    fun refresh() = loadJobs(forceRefresh = true)
}
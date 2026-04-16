package com.hirehuborg.careers.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.Job
import com.hirehuborg.careers.data.repository.SavedJobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────
sealed class SavedJobsState {
    object Loading                            : SavedJobsState()
    object Empty                              : SavedJobsState()
    data class Success(val jobs: List<Job>)   : SavedJobsState()
    data class Error(val message: String)     : SavedJobsState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class SavedJobsViewModel : ViewModel() {

    private val repository = SavedJobsRepository()

    private val _state = MutableStateFlow<SavedJobsState>(SavedJobsState.Loading)
    val state: StateFlow<SavedJobsState> = _state

    // ── Observe real-time saved jobs ──────────────────────────────────────────

    /**
     * Collects the RTDB Flow — UI updates automatically
     * whenever a job is saved or removed anywhere in the app.
     */
    fun observeSavedJobs() {
        viewModelScope.launch {
            repository.observeSavedJobs()
                .catch { e ->
                    _state.value = SavedJobsState.Error(
                        e.message ?: "Could not load saved jobs."
                    )
                }
                .collect { jobs ->
                    _state.value = if (jobs.isEmpty()) {
                        SavedJobsState.Empty
                    } else {
                        SavedJobsState.Success(jobs)
                    }
                }
        }
    }

    // ── Remove a saved job ────────────────────────────────────────────────────
    fun removeJob(jobId: String) {
        viewModelScope.launch {
            repository.removeJob(jobId).onFailure { e ->
                _state.value = SavedJobsState.Error(
                    e.message ?: "Could not remove job."
                )
            }
        }
    }
}
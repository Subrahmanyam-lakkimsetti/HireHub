package com.hirehuborg.careers.ui.viewmodels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.ResumeAnalysis
import com.hirehuborg.careers.domain.usecases.AnalyzeResumeUseCase
import kotlinx.coroutines.launch

// ── State ────────────────────────────────────────────────────────────────────

sealed class AnalysisState {
    object Idle    : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val analysis: ResumeAnalysis) : AnalysisState()
    data class Error(val message: String)            : AnalysisState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class AnalysisViewModel : ViewModel() {

    private val useCase = AnalyzeResumeUseCase()

    private val _state = MutableLiveData<AnalysisState>(AnalysisState.Idle)
    val state: LiveData<AnalysisState> = _state

    fun analyze(resumeText: String, detectedSkills: List<String>) {
        if (_state.value is AnalysisState.Loading) return // prevent double-tap

        _state.value = AnalysisState.Loading

        viewModelScope.launch {
            useCase.execute(resumeText, detectedSkills).fold(
                onSuccess = { analysis ->
                    _state.postValue(AnalysisState.Success(analysis))
                },
                onFailure = { error ->
                    _state.postValue(
                        AnalysisState.Error(error.message ?: "Analysis failed.")
                    )
                }
            )
        }
    }

    fun resetState() {
        _state.value = AnalysisState.Idle
    }
}
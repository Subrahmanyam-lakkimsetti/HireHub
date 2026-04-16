package com.hirehuborg.careers.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.SkillCategory
import com.hirehuborg.careers.data.repository.SkillRepository
import com.hirehuborg.careers.domain.usecases.DetectSkillsUseCase
import kotlinx.coroutines.launch

// ── State ────────────────────────────────────────────────────────────────────

sealed class SkillDetectionState {
    object Idle    : SkillDetectionState()
    object Loading : SkillDetectionState()
    data class Success(
        val categories: List<SkillCategory>,
        val flatSkills: List<String>
    ) : SkillDetectionState()
    data class Error(val message: String) : SkillDetectionState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class SkillViewModel : ViewModel() {

    private val useCase = DetectSkillsUseCase(SkillRepository())

    private val _state = MutableLiveData<SkillDetectionState>(SkillDetectionState.Idle)
    val state: LiveData<SkillDetectionState> = _state

    /**
     * Entry point called from SkillDetectionActivity.
     * Accepts resume text passed via Intent from ResumeUploadActivity.
     */
    fun detectSkills(resumeText: String) {
        _state.value = SkillDetectionState.Loading

        viewModelScope.launch {
            useCase.execute(resumeText).fold(
                onSuccess = { result ->
                    _state.postValue(
                        SkillDetectionState.Success(result.categories, result.flatSkills)
                    )
                },
                onFailure = { error ->
                    _state.postValue(
                        SkillDetectionState.Error(error.message ?: "Skill detection failed.")
                    )
                }
            )
        }
    }
}
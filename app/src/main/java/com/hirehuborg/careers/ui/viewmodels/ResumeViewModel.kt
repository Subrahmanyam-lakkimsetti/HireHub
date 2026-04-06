package com.hirehuborg.careers.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hirehuborg.careers.data.model.ResumeData
import com.hirehuborg.careers.data.repository.ResumeRepository
import kotlinx.coroutines.launch

// ── State sealed class ───────────────────────────────────────────────────────

sealed class ResumeUploadState {
    object Idle : ResumeUploadState()
    data class Progress(val percent: Int, val message: String) : ResumeUploadState()
    data class Success(val resumeData: ResumeData) : ResumeUploadState()
    data class Error(val message: String) : ResumeUploadState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ResumeRepository(application.applicationContext)

    private val _uploadState = MutableLiveData<ResumeUploadState>(ResumeUploadState.Idle)
    val uploadState: LiveData<ResumeUploadState> = _uploadState

    private val _selectedFileName = MutableLiveData<String>("")
    val selectedFileName: LiveData<String> = _selectedFileName

    fun onFileSelected(fileName: String) {
        _selectedFileName.value = fileName
    }

    fun processResume(pdfUri: Uri) {
        _uploadState.value = ResumeUploadState.Progress(0, "Starting...")

        viewModelScope.launch {
            repository.processAndSaveResume(
                pdfUri = pdfUri,
                onProgress = { percent ->
                    val message = progressMessage(percent)
                    _uploadState.postValue(ResumeUploadState.Progress(percent, message))
                }
            ).fold(
                onSuccess = { resumeData ->
                    _uploadState.postValue(ResumeUploadState.Success(resumeData))
                },
                onFailure = { error ->
                    _uploadState.postValue(
                        ResumeUploadState.Error(error.message ?: "Something went wrong.")
                    )
                }
            )
        }
    }

    fun resetState() {
        _uploadState.value = ResumeUploadState.Idle
        _selectedFileName.value = ""
    }

    private fun progressMessage(percent: Int): String = when {
        percent <= 10  -> "Opening PDF..."
        percent <= 50  -> "Extracting text from resume..."
        percent <= 65  -> "Processing data..."
        percent <= 85  -> "Saving to database..."
        percent < 100  -> "Almost done..."
        else           -> "Done!"
    }
}
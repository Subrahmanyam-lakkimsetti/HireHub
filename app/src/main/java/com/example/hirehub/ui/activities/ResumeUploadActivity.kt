package com.example.hirehub.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hirehub.MainActivity
import com.example.hirehub.databinding.ActivityResumeUploadBinding
import com.example.hirehub.ui.viewmodels.ResumeUploadState
import com.example.hirehub.ui.viewmodels.ResumeViewModel
import com.example.hirehub.utils.Constants
import com.example.hirehub.utils.PdfExtractor

class ResumeUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumeUploadBinding
    private val viewModel: ResumeViewModel by viewModels()
    private var selectedPdfUri: Uri? = null

    // ── Activity Result: PDF picker ──────────────────────────────────────────
    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleFileSelected(it) }
        }

    // ── Activity Result: storage permission (Android 12 and below only) ──────
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openPdfPicker()
            else showError("Storage permission is required to select a PDF file.")
        }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumeUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClicks()
        observeViewModel()
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Click listeners ──────────────────────────────────────────────────────
    private fun setupClicks() {
        // Drop zone tapped
        binding.cardPickPdf.setOnClickListener {
            checkPermissionAndPick()
        }

        // "Change" button inside file info card
        binding.tvChangePdf.setOnClickListener {
            clearSelection()
        }

        // Main action button
        binding.btnProcess.setOnClickListener {
            val uri = selectedPdfUri
            if (uri == null) {
                showError("Please select a PDF file first.")
                return@setOnClickListener
            }
            hideError()
            viewModel.processResume(uri)
        }
    }

    // ── Observer ─────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.uploadState.observe(this) { state ->
            when (state) {
                is ResumeUploadState.Idle -> {
                    setUiInteractable(true)
                    hideProgress()
                    hideError()
                }

                is ResumeUploadState.Progress -> {
                    setUiInteractable(false)
                    showProgress(state.percent, state.message)
                    hideError()
                }

                is ResumeUploadState.Success -> {
                    setUiInteractable(true)
                    hideProgress()
                    hideError()
                    Toast.makeText(
                        this,
                        "✅ Resume processed successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    goToAnalysis(state.resumeData.extractedText)
                }

                is ResumeUploadState.Error -> {
                    setUiInteractable(true)
                    hideProgress()
                    showError(state.message)
                }
            }
        }
    }

    // ── Permission & picker ──────────────────────────────────────────────────
    private fun checkPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — system picker needs no permission
            openPdfPicker()
        } else {
            val perm = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
                openPdfPicker()
            } else {
                permissionLauncher.launch(perm)
            }
        }
    }

    private fun openPdfPicker() {
        pdfPickerLauncher.launch("application/pdf")
    }

    // ── File selection handling ───────────────────────────────────────────────
    private fun handleFileSelected(uri: Uri) {
        // Validate size before doing anything else
        val sizeMb = PdfExtractor.getFileSizeMb(this, uri)
        if (sizeMb > Constants.MAX_PDF_SIZE_MB) {
            showError(
                "File too large: ${String.format("%.1f", sizeMb)} MB. " +
                        "Maximum allowed size is ${Constants.MAX_PDF_SIZE_MB.toInt()} MB."
            )
            return
        }

        val fileName = PdfExtractor.getFileName(this, uri)
        selectedPdfUri = uri
        viewModel.onFileSelected(fileName)

        // Swap drop zone for file info card
        binding.cardPickPdf.visibility  = View.GONE
        binding.cardFileInfo.visibility = View.VISIBLE
        binding.tvFileName.text = fileName

        // Activate button
        binding.btnProcess.isEnabled = true
        binding.btnProcess.alpha = 1f

        hideError()
    }

    private fun clearSelection() {
        selectedPdfUri = null
        binding.cardPickPdf.visibility  = View.VISIBLE
        binding.cardFileInfo.visibility = View.GONE
        binding.btnProcess.isEnabled = false
        binding.btnProcess.alpha = 0.5f
        viewModel.resetState()
        hideError()
    }

    // ── UI helpers ───────────────────────────────────────────────────────────
    private fun setUiInteractable(enabled: Boolean) {
        binding.cardPickPdf.isEnabled  = enabled
        binding.cardFileInfo.isEnabled = enabled
        binding.btnProcess.isEnabled   = enabled && selectedPdfUri != null
        binding.btnProcess.alpha       = if (enabled && selectedPdfUri != null) 1f else 0.5f
    }

    private fun showProgress(percent: Int, message: String) {
        binding.cardProgress.visibility       = View.VISIBLE
        binding.tvProgressMessage.text        = message
        binding.progressBarUpload.progress    = percent
        binding.tvProgressPercent.text        = "$percent%"
    }

    private fun hideProgress() {
        binding.cardProgress.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.cardError.visibility = View.VISIBLE
        binding.tvError.text         = message
    }

    private fun hideError() {
        binding.cardError.visibility = View.GONE
    }

    // ── Navigation ───────────────────────────────────────────────────────────
    private fun goToAnalysis(extractedText: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(Constants.EXTRA_RESUME_TEXT, extractedText)
        }
        startActivity(intent)
    }
}
package com.example.hirehub.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.hirehub.data.model.ResumeData
import com.example.hirehub.utils.Constants
import com.example.hirehub.utils.PdfExtractor
import kotlinx.coroutines.tasks.await

class ResumeRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val resumesRef = database.getReference(Constants.RTDB_RESUMES_NODE)
    private val usersRef   = database.getReference(Constants.RTDB_USERS_NODE)

    /**
     * Full pipeline — entirely free, no cloud storage used:
     *
     * Step 1 → Extract text from PDF locally using PDFBox (on-device)
     * Step 2 → Build ResumeData object
     * Step 3 → Save to Realtime Database: /resumes/{uid}
     * Step 4 → Update /users/{uid}/resumeFileName for profile display
     *
     * @param pdfUri     Content URI of the selected PDF
     * @param onProgress Callback with Int (0–100) for progress bar
     * @return           Result<ResumeData>
     */
    suspend fun processAndSaveResume(
        pdfUri: Uri,
        onProgress: (Int) -> Unit
    ): Result<ResumeData> {

        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("You must be logged in to upload a resume."))

        // ── Step 1: Extract text locally ────────────────────────────────────
        onProgress(10)
        val extractResult = PdfExtractor.extractText(context, pdfUri)
        if (extractResult.isFailure) {
            return Result.failure(extractResult.exceptionOrNull()!!)
        }
        val extractedText = extractResult.getOrThrow()
        onProgress(50)

        // ── Step 2: Build model ──────────────────────────────────────────────
        val fileName = PdfExtractor.getFileName(context, pdfUri)
        val resumeData = ResumeData(
            uid           = uid,
            fileName      = fileName,
            extractedText = extractedText,
            uploadedAt    = System.currentTimeMillis()
        )
        onProgress(65)

        // ── Step 3: Save ResumeData to RTDB: /resumes/{uid} ─────────────────
        return try {
            resumesRef.child(uid).setValue(resumeData.toMap()).await()
            onProgress(85)

            // ── Step 4: Update user profile with resume file name ────────────
            usersRef.child(uid).child("resumeFileName").setValue(fileName).await()
            onProgress(100)

            Result.success(resumeData)

        } catch (e: Exception) {
            Result.failure(Exception("Failed to save resume data: ${e.message}"))
        }
    }

    /**
     * Fetches the saved resume for the current user from RTDB.
     * Used in later modules (Analysis, Skill Detection) to reload saved data.
     */
    suspend fun getSavedResume(): Result<ResumeData> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Not logged in."))

        return try {
            val snapshot = resumesRef.child(uid).get().await()
            val data = snapshot.getValue(ResumeData::class.java)
                ?: return Result.failure(Exception("No resume found. Please upload your resume first."))
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(Exception("Could not load resume: ${e.message}"))
        }
    }
}
package com.example.hirehub.utils


import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.hirehub.utils.Constants.MAX_EXTRACTED_CHARS
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExtractor {


    suspend fun extractText(context: Context, pdfUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(pdfUri)
                    ?: return@withContext Result.failure(
                        Exception("Could not open file. Please try again.")
                    )

                inputStream.use { stream ->
                    val document: PDDocument = PDDocument.load(stream)

                    document.use { doc ->
                        // Reject encrypted PDFs
                        if (doc.isEncrypted) {
                            return@withContext Result.failure(
                                Exception("This PDF is password-protected. Please upload an unlocked PDF.")
                            )
                        }

                        // Reject empty documents
                        if (doc.numberOfPages == 0) {
                            return@withContext Result.failure(
                                Exception("PDF has no pages.")
                            )
                        }

                        val stripper = PDFTextStripper().apply {
                            sortByPosition = true   // maintains reading order
                            addMoreFormatting = false
                        }

                        val rawText = stripper.getText(doc)

                        if (rawText.isBlank()) {
                            return@withContext Result.failure(
                                Exception(
                                    "No text could be extracted. " +
                                            "Your PDF may be a scanned image. " +
                                            "Please use a text-based PDF."
                                )
                            )
                        }

                        Result.success(cleanAndCap(rawText))
                    }
                }

            } catch (e: Exception) {
                val userMessage = when {
                    e.message?.contains("password", ignoreCase = true) == true ->
                        "PDF is password protected."
                    e.message?.contains("header", ignoreCase = true) == true ->
                        "File does not appear to be a valid PDF."
                    else -> "Could not read PDF: ${e.message}"
                }
                Result.failure(Exception(userMessage))
            }
        }

    /**
     * Cleans raw PDF text and caps it at MAX_EXTRACTED_CHARS.
     *
     * Steps:
     * 1. Split into lines
     * 2. Trim each line
     * 3. Remove blank-only lines (keep single blank line as paragraph break)
     * 4. Re-join and cap length
     */
    private fun cleanAndCap(raw: String): String {
        val lines = raw.lines()
        val cleaned = buildString {
            var lastWasBlank = false
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    if (!lastWasBlank) {
                        appendLine()   // keep one blank line as separator
                        lastWasBlank = true
                    }
                } else {
                    appendLine(trimmed)
                    lastWasBlank = false
                }
            }
        }.trim()

        return if (cleaned.length > MAX_EXTRACTED_CHARS) {
            cleaned.take(MAX_EXTRACTED_CHARS) + "\n\n[... resume truncated for analysis ...]"
        } else {
            cleaned
        }
    }

    /**
     * Returns file size in MB from a content Uri.
     */
    fun getFileSizeMb(context: Context, uri: Uri): Double {
        return try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) / (1024.0 * 1024.0) else 0.0
                } else 0.0
            } ?: 0.0
        } catch (e: Exception) { 0.0 }
    }

    /**
     * Returns the display filename from a content Uri.
     */
    fun getFileName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else "resume.pdf"
                } else "resume.pdf"
            } ?: "resume.pdf"
        } catch (e: Exception) { "resume.pdf" }
    }
}
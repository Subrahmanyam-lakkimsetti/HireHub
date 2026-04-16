package com.hirehuborg.careers.data.model

import android.print.PrintJobId
import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class Job(
    val id: String          = "",
    val title: String       = "",
    val company: String     = "",
    val location: String    = "",
    val salary: String      = "",
    val description: String = "",
    val applyUrl: String    = "",
    val postedAt: String    = "",
    val tags: List<String>  = emptyList(),
    val source: String      = "",
    var isSaved: Boolean    = false
) : Serializable



// ── Adzuna India response models ─────────────────────────────────────────────
data class AdzunaResponse(
    @SerializedName("results") val results: List<AdzunaJob> = emptyList(),
    @SerializedName("count")   val count: Int = 0
)

data class AdzunaJob(
    @SerializedName("id")           val id: String          = "",
    @SerializedName("title")        val title: String       = "",
    @SerializedName("description")  val description: String = "",
    @SerializedName("redirect_url") val redirectUrl: String = "",
    @SerializedName("created")      val created: String     = "",   // ISO-8601 e.g. "2025-04-01T10:00:00Z"
    @SerializedName("salary_min")   val salaryMin: Double?  = null,
    @SerializedName("salary_max")   val salaryMax: Double?  = null,
    @SerializedName("company")      val company: AdzunaCompany = AdzunaCompany(),
    @SerializedName("location")     val location: AdzunaLocation = AdzunaLocation(),
    @SerializedName("category")     val category: AdzunaCategory = AdzunaCategory()
) {
    fun toJob(): Job {
        val salaryStr = when {
            salaryMin != null && salaryMax != null ->
                "₹${salaryMin.toLong()} – ₹${salaryMax.toLong()}"
            salaryMin != null -> "From ₹${salaryMin.toLong()}"
            else -> ""
        }

        return Job(
            id = "adzuna_$id",
            title = title,
            company = company.displayName.ifBlank { "Unknown" },
            location = location.displayName.ifBlank { "India" },
            salary = salaryStr,
            description = description,
            applyUrl = redirectUrl,
            postedAt = created.ifBlank { "0" },   // 🔥 FIX
            tags = listOf(category.label).filter { it.isNotBlank() }, // 🔥 FIX
            source = "adzuna"
        )
    }

    // formats 1200000 → "12,00,000" (Indian number format)
    private fun Long.formatINR(): String {
        val s = this.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$last3"
    }
}

data class AdzunaCompany(
    @SerializedName("display_name") val displayName: String = ""
)

data class AdzunaLocation(
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("area")         val area: List<String>  = emptyList()
)

data class AdzunaCategory(
    @SerializedName("label") val label: String = "",
    @SerializedName("tag")   val tag: String   = ""
)

// ── JSearch (RapidAPI) response models ───────────────────────────────────────
data class JSearchResponse(
    @SerializedName("data")   val data: List<JSearchJob> = emptyList(),
    @SerializedName("status") val status: String         = ""
)

data class JSearchJob(
    @SerializedName("jobId") val jobId: String? = null,
    @SerializedName("job_title") val jobTitle: String? = null,
    @SerializedName("employer_name") val employerName: String? = null,
    @SerializedName("job_city") val jobCity: String? = null,
    @SerializedName("job_state") val jobState: String? = null,
    @SerializedName("job_description") val jobDescription: String? = null,
    @SerializedName("job_apply_link") val jobApplyLink: String? = null,
    @SerializedName("job_posted_at_timestamp")  val postedAtTimestamp: Long = 0L,
    @SerializedName("job_min_salary")           val minSalary: Double?   = null,
    @SerializedName("job_max_salary")           val maxSalary: Double?   = null,
    @SerializedName("job_required_skills")      val requiredSkills: List<String>? = null,
    @SerializedName("job_highlights")           val highlights: JSearchHighlights? = null,
    @SerializedName("job_employment_type")      val employmentType: String? = null
) {
    fun toJob(): Job {
        val locationParts = listOfNotNull(
            jobCity?.ifBlank { null },
            jobState?.ifBlank { null }
        )

        val locationStr = locationParts.joinToString(", ").ifBlank { "India" }

        val salaryStr = when {
            minSalary != null && maxSalary != null ->
                "₹${minSalary.toLong()} – ₹${maxSalary.toLong()}"
            else -> ""
        }

        val skills = requiredSkills
            ?: highlights?.qualifications?.take(5)
            ?: emptyList()

        return Job(
            id = "jsearch_${jobId ?: ""}",
            title = jobTitle ?: "",
            company = employerName?.takeIf { it.isNotBlank() } ?: "Unknown",
            location = locationStr,
            salary = "",
            description = jobDescription ?: "",
            applyUrl = jobApplyLink ?: "",
            postedAt = postedAtTimestamp.toString(),
            tags = skills.filter { !it.isNullOrBlank() }, // 🔥 SAFE
            source = "jsearch"
        )
    }
}

data class JSearchHighlights(
    @SerializedName("Qualifications") val qualifications: List<String> = emptyList(),
    @SerializedName("Responsibilities") val responsibilities: List<String> = emptyList()
)
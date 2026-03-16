package com.example.hirehub.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Unified Job model used across the app
data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val salary: String = "",
    val description: String = "",
    val applyUrl: String = "",
    val postedAt: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "",       // "remotive", "arbeitnow", "adzuna"
    var isSaved: Boolean = false
) : Serializable

// ─── Remotive API response models ───────────────────────────────────────────

data class RemotiveResponse(
    @SerializedName("jobs") val jobs: List<RemotiveJob>
)

data class RemotiveJob(
    @SerializedName("id") val id: Int,
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("company_name") val companyName: String,
    @SerializedName("candidate_required_location") val location: String,
    @SerializedName("salary") val salary: String,
    @SerializedName("description") val description: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("publication_date") val publicationDate: String
) {
    fun toJob() = Job(
        id = "remotive_$id",
        title = title,
        company = companyName,
        location = location.ifEmpty { "Remote" },
        salary = salary,
        description = description,
        applyUrl = url,
        postedAt = publicationDate,
        tags = tags,
        source = "remotive"
    )
}

// ─── Arbeitnow API response models ──────────────────────────────────────────

data class ArbeitnowResponse(
    @SerializedName("data") val data: List<ArbeitnowJob>
)

data class ArbeitnowJob(
    @SerializedName("slug") val slug: String,
    @SerializedName("company_name") val companyName: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("remote") val remote: Boolean,
    @SerializedName("url") val url: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("job_types") val jobTypes: List<String>,
    @SerializedName("location") val location: String,
    @SerializedName("created_at") val createdAt: Long
) {
    fun toJob() = Job(
        id = "arbeitnow_$slug",
        title = title,
        company = companyName,
        location = if (remote) "Remote" else location,
        salary = "",
        description = description,
        applyUrl = url,
        postedAt = createdAt.toString(),
        tags = tags,
        source = "arbeitnow"
    )
}

// ─── Adzuna API response models ──────────────────────────────────────────────

data class AdzunaResponse(
    @SerializedName("results") val results: List<AdzunaJob>
)

data class AdzunaJob(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("company") val company: AdzunaCompany,
    @SerializedName("location") val location: AdzunaLocation,
    @SerializedName("salary_min") val salaryMin: Double?,
    @SerializedName("salary_max") val salaryMax: Double?,
    @SerializedName("description") val description: String,
    @SerializedName("redirect_url") val redirectUrl: String,
    @SerializedName("created") val created: String
) {
    fun toJob() = Job(
        id = "adzuna_$id",
        title = title,
        company = company.displayName,
        location = location.displayName,
        salary = buildSalaryString(salaryMin, salaryMax),
        description = description,
        applyUrl = redirectUrl,
        postedAt = created,
        source = "adzuna"
    )

    private fun buildSalaryString(min: Double?, max: Double?): String {
        return when {
            min != null && max != null -> "₹${min.toInt()} - ₹${max.toInt()}"
            min != null -> "₹${min.toInt()}+"
            else -> ""
        }
    }
}

data class AdzunaCompany(
    @SerializedName("display_name") val displayName: String
)

data class AdzunaLocation(
    @SerializedName("display_name") val displayName: String
)

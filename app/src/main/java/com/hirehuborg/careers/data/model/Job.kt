package com.hirehuborg.careers.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ── Unified Job model used across the entire app ─────────────────────────────
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

// ── Remotive response models ──────────────────────────────────────────────────
data class RemotiveResponse(
    @SerializedName("jobs") val jobs: List<RemotiveJob> = emptyList()
)

data class RemotiveJob(
    @SerializedName("id")                           val id: Int             = 0,
    @SerializedName("url")                          val url: String         = "",
    @SerializedName("title")                        val title: String       = "",
    @SerializedName("company_name")                 val companyName: String = "",
    @SerializedName("candidate_required_location")  val location: String    = "",
    @SerializedName("salary")                       val salary: String      = "",
    @SerializedName("description")                  val description: String = "",
    @SerializedName("tags")                         val tags: List<String>  = emptyList(),
    @SerializedName("publication_date")             val publicationDate: String = ""
) {
    fun toJob() = Job(
        id          = "remotive_$id",
        title       = title,
        company     = companyName,
        location    = location.ifBlank { "Remote" },
        salary      = salary,
        description = description,
        applyUrl    = url,
        postedAt    = publicationDate,
        tags        = tags,
        source      = "remotive"
    )
}

// ── Arbeitnow response models ─────────────────────────────────────────────────
data class ArbeitnowResponse(
    @SerializedName("data") val data: List<ArbeitnowJob> = emptyList()
)

data class ArbeitnowJob(
    @SerializedName("slug")         val slug: String        = "",
    @SerializedName("company_name") val companyName: String = "",
    @SerializedName("title")        val title: String       = "",
    @SerializedName("description")  val description: String = "",
    @SerializedName("remote")       val remote: Boolean     = false,
    @SerializedName("url")          val url: String         = "",
    @SerializedName("tags")         val tags: List<String>  = emptyList(),
    @SerializedName("location")     val location: String    = "",
    @SerializedName("created_at")   val createdAt: Long     = 0L
) {
    fun toJob() = Job(
        id          = "arbeitnow_$slug",
        title       = title,
        company     = companyName,
        location    = if (remote) "Remote" else location.ifBlank { "Remote" },
        salary      = "",
        description = description,
        applyUrl    = url,
        postedAt    = createdAt.toString(),
        tags        = tags,
        source      = "arbeitnow"
    )
}
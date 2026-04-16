package com.hirehuborg.careers.data.remote

import com.hirehuborg.careers.data.model.AdzunaResponse
import com.hirehuborg.careers.data.model.JSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// ── Adzuna India — Free tier, needs app_id + app_key (register free at developer.adzuna.com)
// Base URL: https://api.adzuna.com/v1/api/
interface AdzunaApiService {
    @GET("jobs/in/search/{page}")
    suspend fun getJobs(
        @Path("page")           page: Int    = 1,
        @Query("app_id")        appId: String,
        @Query("app_key")       appKey: String,
        @Query("what")          query: String = "software developer",
        @Query("results_per_page") resultsPerPage: Int = 40,
        @Query("sort_by")       sortBy: String = "date",       // newest first
        @Query("max_days_old")  maxDaysOld: Int = 30,          // only active/recent
        @Query("content-type")  contentType: String = "application/json"
    ): Response<AdzunaResponse>
}

// ── JSearch (RapidAPI) — Free tier: 500 calls/month, aggregates LinkedIn + Indeed India
// Base URL: https://jsearch.p.rapidapi.com/
interface JSearchApiService {
    @GET("search")
    suspend fun getJobs(
        @Header("X-RapidAPI-Key")  apiKey: String,
        @Header("X-RapidAPI-Host") host: String = "jsearch.p.rapidapi.com",
        @Query("query")            query: String = "software engineer India",
        @Query("page")             page: Int = 1,
        @Query("num_pages")        numPages: Int = 1,
        @Query("date_posted")      datePosted: String = "month",  // today/3days/week/month
        @Query("country")          country: String = "in"
    ): Response<JSearchResponse>
}
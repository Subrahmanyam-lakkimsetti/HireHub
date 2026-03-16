package com.example.hirehub.data.remote

import com.example.hirehub.data.model.AdzunaResponse
import com.example.hirehub.data.model.ArbeitnowResponse
import com.example.hirehub.data.model.RemotiveResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Remotive API — free, no key needed
interface RemotiveApiService {
    @GET("remote-jobs")
    suspend fun getRemoteJobs(
        @Query("search") search: String = "",
        @Query("limit") limit: Int = 20
    ): Response<RemotiveResponse>
}

// Arbeitnow API — free, no key needed
interface ArbeitnowApiService {
    @GET("jobboard-api")
    suspend fun getJobs(
        @Query("page") page: Int = 1
    ): Response<ArbeitnowResponse>
}

// Adzuna API — free tier (register at adzuna.com for app_id + app_key)
interface AdzunaApiService {
    @GET("jobs/{country}/search/{page}")
    suspend fun searchJobs(
        @Path("country") country: String = "in",
        @Path("page") page: Int = 1,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("results_per_page") resultsPerPage: Int = 20,
        @Query("what") keyword: String = "developer",
        @Query("where") location: String = "India"
    ): Response<AdzunaResponse>
}
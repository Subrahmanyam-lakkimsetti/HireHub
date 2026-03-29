package com.hirehuborg.careers.data.remote

import com.hirehuborg.careers.data.model.ArbeitnowResponse
import com.hirehuborg.careers.data.model.RemotiveResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// ── Remotive — free, no API key needed ───────────────────────────────────────
interface RemotiveApiService {
    @GET("remote-jobs")
    suspend fun getJobs(
        @Query("search")  search: String = "",
        @Query("limit")   limit: Int     = 30
    ): Response<RemotiveResponse>
}

// ── Arbeitnow — free, no API key needed ──────────────────────────────────────
interface ArbeitnowApiService {
    @GET("jobboard-api")
    suspend fun getJobs(
        @Query("page") page: Int = 1
    ): Response<ArbeitnowResponse>
}
package com.hirehuborg.careers.network

import com.hirehuborg.careers.data.remote.AdzunaApiService
import com.hirehuborg.careers.data.remote.JSearchApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── Adzuna India ──────────────────────────────────────────────────────────
    val adzunaApi: AdzunaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.adzuna.com/v1/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdzunaApiService::class.java)
    }

    // ── JSearch (RapidAPI) ────────────────────────────────────────────────────
    val jSearchApi: JSearchApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsearch.p.rapidapi.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JSearchApiService::class.java)
    }
}
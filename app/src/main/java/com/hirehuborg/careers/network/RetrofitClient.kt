package com.hirehuborg.careers.network

import com.hirehuborg.careers.data.remote.ArbeitnowApiService
import com.hirehuborg.careers.data.remote.RemotiveApiService
import com.hirehuborg.careers.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val remotiveApi: RemotiveApiService by lazy {
        buildRetrofit(Constants.REMOTIVE_BASE_URL)
            .create(RemotiveApiService::class.java)
    }

    val arbeitnowApi: ArbeitnowApiService by lazy {
        buildRetrofit(Constants.ARBEITNOW_BASE_URL)
            .create(ArbeitnowApiService::class.java)
    }
}
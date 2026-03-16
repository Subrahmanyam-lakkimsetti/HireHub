package com.example.hirehub.network

import com.google.ai.client.generativeai.GenerativeModel
import com.example.hirehub.utils.Constants
import com.example.hirehub.BuildConfig

object GeminiClient {

    val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = Constants.GEMINI_MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
}
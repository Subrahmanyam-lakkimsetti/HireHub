package com.hirehuborg.careers.network

import com.google.ai.client.generativeai.GenerativeModel
import com.hirehuborg.careers.utils.Constants
import com.hirehuborg.careers.BuildConfig

object GeminiClient {

    val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = Constants.GEMINI_MODEL,
            apiKey    = BuildConfig.GEMINI_API_KEY,

        )
    }
}
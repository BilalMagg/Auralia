// LlamaRepository.kt
package com.voiceassistant.repository

import android.util.Log
import com.voiceassistant.model.LlamaRequest
import com.voiceassistant.network.LlamaApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LlamaRepository {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:11434/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(LlamaApi::class.java)

    suspend fun getResponse(prompt: String): String {
        try {
            Log.d("LlamaRepository", "Sending request to Ollama with prompt: $prompt")
            val request = LlamaRequest(model = "gemma3n:e2b", prompt = prompt)
            val response = api.generate(request)
            Log.d("LlamaRepository", "Received response: ${response.response}")
            return response.response
        } catch (e: Exception) {
            Log.e("LlamaRepository", "Error calling Ollama API", e)
            when {
                e.message?.contains("Failed to connect") == true -> {
                    throw Exception("Cannot connect to Ollama server. Please make sure Ollama is running on your computer.")
                }
                e.message?.contains("timeout") == true -> {
                    throw Exception("Request timed out. The model might be taking too long to respond.")
                }
                else -> {
                    throw Exception("Network error: ${e.message}")
                }
            }
        }
    }
}

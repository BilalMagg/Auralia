// LlamaRepository.kt
package com.voiceassistant.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.voiceassistant.config.NetworkConfig
import com.voiceassistant.model.LlamaRequest
import com.voiceassistant.model.LlamaResponse
import com.voiceassistant.network.LlamaApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LlamaRepository(private val context: Context) {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for streaming
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        val baseUrl = NetworkConfig.getOllamaUrl(context)
        Log.d("LlamaRepository", "Using Ollama server URL: $baseUrl")
        
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api: LlamaApi by lazy { retrofit.create(LlamaApi::class.java) }
    private val gson = Gson()

    suspend fun getResponse(prompt: String, model: String = "gemma3n:e2b", useStreaming: Boolean = false): String {
        return if (useStreaming) {
            // For streaming, we should use the Flow directly, not collect it here
            // This method is mainly for non-streaming responses
            getNonStreamingResponse(prompt, model)
        } else {
            getNonStreamingResponse(prompt, model)
        }
    }


    fun getStreamingResponse(prompt: String, model: String = "gemma3n:e2b"): Flow<String> = flow {
        try {
            Log.d("LlamaRepository", "Sending streaming request to Ollama with prompt: $prompt")
            val request = LlamaRequest(model = model, prompt = prompt, stream = true)
            val response = api.generateStream(request)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val source = responseBody.source()
                    var fullResponse = ""
                    
                    Log.d("LlamaRepository", "Starting to read streaming response")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        if (line != null && line.isNotEmpty()) {
                            try {
                                Log.d("LlamaRepository", "Received line: $line")
                                val llamaResponse = gson.fromJson(line, LlamaResponse::class.java)
                                fullResponse += llamaResponse.response
                                emit(fullResponse)
                                
                                if (llamaResponse.done) {
                                    Log.d("LlamaRepository", "Streaming completed")
                                    break
                                }
                            } catch (e: Exception) {
                                Log.e("LlamaRepository", "Error parsing streaming response line: $line", e)
                            }
                        }
                    }
                } else {
                    Log.e("LlamaRepository", "Response body is null")
                    throw Exception("Empty response from server")
                }
            } else {
                Log.e("LlamaRepository", "Streaming request failed with code: ${response.code()}")
                throw Exception("Streaming request failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("LlamaRepository", "Error in streaming request", e)
            throw handleException(e)
        }
    }

    private suspend fun getNonStreamingResponse(prompt: String, model: String): String {
        try {
            Log.d("LlamaRepository", "Sending non-streaming request to Ollama with prompt: $prompt")
            val request = LlamaRequest(model = model, prompt = prompt, stream = false)
            val response = api.generate(request)
            Log.d("LlamaRepository", "Received response: ${response.response}")
            return response.response
        } catch (e: Exception) {
            Log.e("LlamaRepository", "Error calling Ollama API", e)
            throw handleException(e)
        }
    }

    private fun handleException(e: Exception): Exception {
        return when {
            e.message?.contains("Failed to connect") == true -> {
                Exception("Cannot connect to Ollama server. Please make sure Ollama is running on your computer.")
            }
            e.message?.contains("timeout") == true -> {
                Exception("Request timed out. The model might be taking too long to respond.")
            }
            else -> {
                Exception("Network error: ${e.message}")
            }
        }
    }
    
    /**
     * Update the server URL and recreate the API client
     */
    fun updateServerUrl(newUrl: String) {
        NetworkConfig.setOllamaUrl(context, newUrl)
        Log.d("LlamaRepository", "Server URL updated to: $newUrl")
        // Note: The retrofit instance will be recreated on next API call due to lazy initialization
    }
    
    /**
     * Get current server URL
     */
    fun getCurrentServerUrl(): String {
        return NetworkConfig.getOllamaUrl(context)
    }
    
    /**
     * Reset to default server URL
     */
    fun resetToDefaultUrl() {
        NetworkConfig.resetToDefault(context)
        Log.d("LlamaRepository", "Reset to default server URL")
    }
    
    /**
     * Check if using custom URL
     */
    fun isUsingCustomUrl(): Boolean {
        return NetworkConfig.isUsingCustomUrl(context)
    }
}

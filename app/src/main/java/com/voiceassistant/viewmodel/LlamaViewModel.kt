// LlamaViewModel.kt
package com.voiceassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.repository.LlamaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LlamaViewModel : ViewModel() {
    private val repository = LlamaRepository()

    private val _response = MutableStateFlow("")
    val response = _response.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _useStreaming = MutableStateFlow(false)
    val useStreaming = _useStreaming.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemma3n:e2b")
    val selectedModel = _selectedModel.asStateFlow()

    fun sendPrompt(prompt: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _response.value = ""
                
                if (_useStreaming.value) {
                    // Handle streaming with proper dispatcher
                    withContext(Dispatchers.IO) {
                        repository.getStreamingResponse(prompt, _selectedModel.value).collect { partialResponse ->
                            // Switch back to main thread to update UI
                            withContext(Dispatchers.Main) {
                                _response.value = partialResponse
                            }
                        }
                    }
                } else {
                    // Handle non-streaming with proper dispatcher
                    val result = withContext(Dispatchers.IO) {
                        repository.getResponse(prompt, _selectedModel.value, false)
                    }
                    _response.value = result
                }
            } catch (e: Exception) {
                _response.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleStreaming() {
        _useStreaming.value = !_useStreaming.value
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }
}

// LlamaViewModel.kt
package com.voiceassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.repository.LlamaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LlamaViewModel : ViewModel() {
    private val repository = LlamaRepository()

    private val _response = MutableStateFlow("")
    val response = _response.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendPrompt(prompt: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _response.value = ""
                val result = repository.getResponse(prompt)
                _response.value = result
            } catch (e: Exception) {
                _response.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

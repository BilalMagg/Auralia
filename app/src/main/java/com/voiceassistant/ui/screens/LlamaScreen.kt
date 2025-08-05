// LlamaScreen.kt - Version complète finale
package com.voiceassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voiceassistant.viewmodel.LlamaViewModel
import com.voiceassistant.ai.AIAssistantManager

@Composable
fun LlamaScreen(
    onBackClick: () -> Unit = {},
    aiAssistantManager: AIAssistantManager? = null
) {
    val vm: LlamaViewModel = viewModel()
    val result by vm.response.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val useStreaming by vm.useStreaming.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }

    // États pour les résultats IA
    var aiExecutionResult by remember { mutableStateOf("") }
    var generatedJsonText by remember { mutableStateOf("") }
    var generatedActionsList by remember { mutableStateOf(listOf<String>()) }
    var showResults by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
            Text(
                text = "🤖 AI Generic Assistant",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showModelSelector = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Model: $selectedModel", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Streaming", color = Color.Black, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = useStreaming, onCheckedChange = { vm.toggleStreaming() })
            }
        }

        // Model selector dialog
        if (showModelSelector) {
            AlertDialog(
                onDismissRequest = { showModelSelector = false },
                title = { Text("Select Model", color = Color.Black) },
                text = {
                    Column {
                        listOf("gemma3n:e2b", "llama3.2", "mistral", "codellama").forEach { model ->
                            TextButton(
                                onClick = {
                                    vm.setModel(model)
                                    showModelSelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = model,
                                    color = if (selectedModel == model) Color.Blue else Color.Black
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModelSelector = false }) {
                        Text("Cancel", color = Color.Black)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧠 Pure AI Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "No hardcoded actions! Gemma3 analyzes your request and decides what to do.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try: 'visit google.com', 'calculate 25×67', 'turn on airplane mode', 'send location to John'",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter ANY request - AI will figure it out", color = Color.Gray) },
            placeholder = { Text("e.g., 'I want to visit stackoverflow.com'") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Single unified button
        Button(
            onClick = {
                if (prompt.isNotBlank()) {
                    isExecuting = true
                    showResults = false

                    // Reset previous results
                    aiExecutionResult = ""
                    generatedJsonText = ""
                    generatedActionsList = emptyList()

                    // Send to model for chat response
                    vm.sendPrompt(prompt)
                    // Execute with AI and capture REAL details
                    if (aiAssistantManager != null) {
                        // Check if the detailed method exists, otherwise use simple method
                        try {
                            aiAssistantManager.processVoiceCommandWithDetails(prompt) { success, message, realJson, realActions ->
                                aiExecutionResult = if (success) {
                                    "✅ Success: $message"
                                } else {
                                    "❌ Error: $message"
                                }

                                generatedJsonText = realJson
                                generatedActionsList = realActions
                                showResults = true
                                isExecuting = false
                            }
                        } catch (e: Exception) {
                            // Fallback to simple method if detailed doesn't exist
                            aiAssistantManager.processVoiceCommand(prompt) { success, message ->
                                aiExecutionResult = if (success) {
                                    "✅ Success: $message"
                                } else {
                                    "❌ Error: $message"
                                }

                                generatedJsonText = "JSON capture not available in simple mode"
                                generatedActionsList = listOf("Action details not available - using simple mode")
                                showResults = true
                                isExecuting = false
                            }
                        }
                    } else {
                        aiExecutionResult = "❌ AI Assistant not available"
                        generatedJsonText = """{"success": false, "message": "AI service unavailable"}"""
                        generatedActionsList = listOf("Error: AI Assistant Manager not initialized")
                        showResults = true
                        isExecuting = false
                    }
                }
            },
            enabled = prompt.isNotBlank() && !isLoading && !isExecuting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading || isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                when {
                    isExecuting -> "🧠 AI is thinking & executing..."
                    isLoading -> "💭 Processing with model..."
                    else -> "🚀 Analyze & Execute with AI"
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Results Section
        if (showResults) {
            // Execution Result
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (aiExecutionResult.startsWith("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI Execution Result:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiExecutionResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generated JSON & Actions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧠 AI Analysis & Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raw JSON from Gemma3
                    Text(
                        text = "📋 Raw Gemma3 Response:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Text(
                            text = generatedJsonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Generated Actions
                    Text(
                        text = "🎬 AI Generated Actions (${generatedActionsList.size}):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    if (generatedActionsList.isNotEmpty()) {
                        generatedActionsList.forEachIndexed { index, action ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. $action",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No actions generated",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Model Chat Response
        if (result.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬 Model Chat Response:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (useStreaming) {
                            Text("Streaming", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Status indicators
        if (aiAssistantManager == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ AI Assistant Not Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "The AI Assistant Manager is not initialized. Only chat mode will work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
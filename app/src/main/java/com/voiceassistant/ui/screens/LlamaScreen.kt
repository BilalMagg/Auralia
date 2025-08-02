// LlamaScreen.kt
package com.voiceassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voiceassistant.viewmodel.LlamaViewModel

@Composable
fun LlamaScreen(
    onBackClick: () -> Unit = {}
) {
    val vm: LlamaViewModel = viewModel()
    val result by vm.response.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val useStreaming by vm.useStreaming.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp) // Add top padding to avoid status bar
    ) {
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
                text = "Gemma 3N E2B Chat",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model Selector
            OutlinedButton(
                onClick = { showModelSelector = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Model: $selectedModel", color = Color.Black)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Streaming Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Streaming",
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = useStreaming,
                    onCheckedChange = { vm.toggleStreaming() }
                )
            }
        }

        // Model Selection Dialog
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

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter your prompt", color = Color.Gray) },
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

        Button(
            onClick = { 
                if (prompt.isNotBlank()) {
                    vm.sendPrompt(prompt)
                }
            },
            enabled = prompt.isNotBlank() && !isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Sending..." else "Send to ${selectedModel}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (result.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Response:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (useStreaming) {
                            Text(
                                text = "Streaming",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
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
    }
}

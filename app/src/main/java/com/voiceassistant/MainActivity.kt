package com.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voiceassistant.service.VoiceAssistantService
import com.voiceassistant.ui.theme.VoiceAssistantTheme
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var textToSpeech: TextToSpeech
    private var isServiceRunning by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startVoiceAssistantService()
        } else {
            speakText("Permissions are required for the voice assistant to work properly")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DebugTest", "MainActivity onCreate called")

        textToSpeech = TextToSpeech(this, this)
        
        setContent {
            VoiceAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceRunning = isServiceRunning,
                        onToggleService = { toggleService() },
                        onSpeakInstructions = { speakInstructions() }
                    )
                }
            }
        }
        
        checkPermissions()
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startVoiceAssistantService()
        }
    }
    
    private fun startVoiceAssistantService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        startForegroundService(intent)
        isServiceRunning = true
        speakText("Voice assistant is now active. Say 'Hey Gemma' to start")
    }
    
    private fun stopVoiceAssistantService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        stopService(intent)
        isServiceRunning = false
        speakText("Voice assistant stopped")
    }
    
    private fun toggleService() {
        if (isServiceRunning) {
            stopVoiceAssistantService()
        } else {
            startVoiceAssistantService()
        }
    }
    
    private fun speakInstructions() {
        val instructions = """
            Welcome to your voice assistant. Here are the available commands:
            Say 'Hey Gemma' followed by:
            - Call [contact name] to make a phone call
            - Send message to [contact name] saying [message] to send SMS
            - Open [app name] to launch applications
            - What time is it for current time
            - Read notifications to hear your notifications
            - Help for assistance
        """.trimIndent()
        speakText(instructions)
    }
    
    private fun speakText(text: String) {
        if (::textToSpeech.isInitialized && textToSpeech.isSpeaking.not()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            speakText("Voice assistant initialized successfully")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}

@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    onSpeakInstructions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Voice Assistant for Accessibility",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics {
                contentDescription = "Voice Assistant for Accessibility - Main heading"
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isServiceRunning) "Assistant is Active" else "Assistant is Inactive",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isServiceRunning) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        contentDescription = if (isServiceRunning) 
                            "Voice assistant is currently active and listening" 
                        else 
                            "Voice assistant is currently inactive"
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onToggleService,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (isServiceRunning) 
                                "Stop voice assistant service" 
                            else 
                                "Start voice assistant service"
                        }
                ) {
                    Text(if (isServiceRunning) "Stop Assistant" else "Start Assistant")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onSpeakInstructions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Hear voice commands and instructions"
                }
        ) {
            Text("Hear Instructions")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Say 'Hey Gemma' to activate voice commands",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics {
                contentDescription = "To use the assistant, say the wake word 'Hey Gemma' followed by your command"
            }
        )
    }
}

package com.voiceassistant

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    private var recognizedText by mutableStateOf("")
    private var hasAudioPermission by mutableStateOf(false)
    private var wakeWordDetected by mutableStateOf(false)
    private var audioDetected by mutableStateOf(false)

    // --- Pour binder le service et appeler ses méthodes ---
    private var voiceAssistantService: VoiceAssistantService? = null
    private var isBound = false

    // ServiceConnection pour la liaison avec VoiceAssistantService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? VoiceAssistantService.LocalBinder
            voiceAssistantService = localBinder?.getService()
            isBound = true
            Log.d("MainActivity", "Service connected")
            
            // Set up wake word callback
            localBinder?.setWakeWordCallback { detected ->
                runOnUiThread {
                    wakeWordDetected = detected
                    if (detected) {
                        Toast.makeText(this@MainActivity, "Wake word detected! 🎉", Toast.LENGTH_SHORT).show()
                        // Reset after 3 seconds
                        Handler(Looper.getMainLooper()).postDelayed({
                            wakeWordDetected = false
                        }, 3000)
                    }
                }
            }
            
            // Set up audio detection callback
            localBinder?.setAudioDetectionCallback { detected ->
                runOnUiThread {
                    audioDetected = detected
                }
            }
            
            // Start listening for wake word detection
            startWakeWordDetection()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            voiceAssistantService = null
            Log.d("MainActivity", "Service disconnected")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val essentialPermissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )

        val essentialPermissionsGranted = essentialPermissions.all {
            permissions[it] == true || ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (essentialPermissionsGranted) {
            if (permissions.containsKey(Manifest.permission.RECORD_AUDIO) && permissions[Manifest.permission.RECORD_AUDIO] == true || hasRecordAudioPermission()) {
                startVoiceAssistantService() // Démarre le service et liaison
            } else {
                speakText("Recording audio permission is essential for the voice assistant.")
                Toast.makeText(this, "Audio recording permission is required.", Toast.LENGTH_LONG).show()
            }
        } else {
            speakText("Some essential permissions were not granted. The voice assistant may not work properly.")
            Toast.makeText(this, "Not all essential permissions were granted.", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                        recognizedText = recognizedText,
                        hasAudioPermission = hasAudioPermission,
                        wakeWordDetected = wakeWordDetected,
                        audioDetected = audioDetected,
                        onToggleService = { toggleService() },
                        onSpeakInstructions = { speakInstructions() },
                        onTestSpeechRecognition = { testSpeechRecognition() },
                        onTestWakeWordDetection = { testWakeWordDetection() },
                        onShowDetailedStatus = { showDetailedStatus() }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun hasRecordAudioPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = granted
        return granted
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.FOREGROUND_SERVICE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startVoiceAssistantService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startVoiceAssistantService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        startForegroundService(intent)
        // Bind au service pour appeler la méthode testSpeechRecognition
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isServiceRunning = true
        speakText("Voice assistant is now active. Say 'Hi Aura' to start")
    }

    private fun stopVoiceAssistantService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            voiceAssistantService = null
        }
        val intent = Intent(this, VoiceAssistantService::class.java)
        stopService(intent)
        isServiceRunning = false
        wakeWordDetected = false
        speakText("Voice assistant stopped")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleService() {
        if (isServiceRunning) {
            stopVoiceAssistantService()
        } else {
            val allPermissionsGranted = listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
            ).all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                startVoiceAssistantService()
            } else {
                checkPermissions()
                Toast.makeText(this, "Permissions are required to start the assistant.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startWakeWordDetection() {
        // This will be handled by the VoiceAssistantService
        // We just need to listen for wake word detection events
        Log.d("MainActivity", "Wake word detection started via service")
    }

    private fun speakInstructions() {
        val instructions = """
            Welcome to your voice assistant. Here are the available commands:
            Say 'Hi Aura' followed by:
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
        if (::textToSpeech.isInitialized && !textToSpeech.isSpeaking) {
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
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            voiceAssistantService = null
        }
    }

    // Fonction pour tester la reconnaissance vocale directement (sans passer par le service)
    private fun testSpeechRecognitionDirect() {
        if (!hasRecordAudioPermission()) {
            Toast.makeText(applicationContext, "Permission micro requise", Toast.LENGTH_SHORT).show()
            return
        }
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(applicationContext, "Erreur reconnaissance vocale: $error", Toast.LENGTH_SHORT).show()
                recognizedText = "Erreur reconnaissance vocale: $error"
                speechRecognizer.destroy()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Toast.makeText(applicationContext, "Vous avez dit: ${matches[0]}", Toast.LENGTH_LONG).show()
                    recognizedText = matches[0]
                } else {
                    Toast.makeText(applicationContext, "Aucune parole reconnue", Toast.LENGTH_SHORT).show()
                    recognizedText = "Aucune parole reconnue"
                }
                speechRecognizer.destroy()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    // Modifie la fonction testSpeechRecognition pour appeler la version directe aussi
    private fun testSpeechRecognition() {
        testSpeechRecognitionDirect()
        if (isBound && voiceAssistantService != null) {
            voiceAssistantService?.testSpeechRecognition()
        } else {
            Toast.makeText(this, "Service not bound. Start the assistant first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testWakeWordDetection() {
        if (isBound && voiceAssistantService != null) {
            voiceAssistantService?.testWakeWordDetection()
            val status = voiceAssistantService?.getWakeWordStatus()
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Service not bound. Start the assistant first.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDetailedStatus() {
        if (isBound && voiceAssistantService != null) {
            val status = voiceAssistantService?.getDetailedStatus()
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Service not bound. Start the assistant first.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    recognizedText: String,
    hasAudioPermission: Boolean,
    wakeWordDetected: Boolean,
    audioDetected: Boolean,
    onToggleService: () -> Unit,
    onSpeakInstructions: () -> Unit,
    onTestSpeechRecognition: () -> Unit,
    onTestWakeWordDetection: () -> Unit,
    onShowDetailedStatus: () -> Unit
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

                Spacer(modifier = Modifier.height(16.dp))

                // Nouveau bouton pour tester la reconnaissance vocale directement
                OutlinedButton(
                    onClick = onTestSpeechRecognition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Test speech recognition manually"
                        }
                ) {
                    Text("Test Speech Recognition")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test wake word detection
                OutlinedButton(
                    onClick = onTestWakeWordDetection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Test wake word detection"
                        }
                ) {
                    Text("Test Wake Word Detection")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show detailed status
                OutlinedButton(
                    onClick = onShowDetailedStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Show detailed service status"
                        }
                ) {
                    Text("Show Service Status")
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

        if (recognizedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Audio detection status
        if (isServiceRunning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (audioDetected) 
                        MaterialTheme.colorScheme.secondaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (audioDetected) "🎤 Audio Detected" else "🔇 No Audio",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = if (audioDetected) 
                        MaterialTheme.colorScheme.onSecondaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Wake word detection status
        if (isServiceRunning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (wakeWordDetected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (wakeWordDetected) "Wake Word Detected! 🎉" else "Listening for 'Hi Aura'...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = if (wakeWordDetected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Say 'Hi Aura' to activate voice commands",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics {
                contentDescription = "To use the assistant, say the wake word 'Hi Aura' followed by your command"
            }
        )

        // Affichage de la permission micro
        Text(
            text = if (hasAudioPermission) "Microphone permission: GRANTED" else "Microphone permission: REFUSED",
            color = if (hasAudioPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (!hasAudioPermission) {
            Text(
                text = "Please enable microphone permission in your phone settings.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

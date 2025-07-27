package com.voiceassistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.voiceassistant.MainActivity
import com.voiceassistant.R
import com.voiceassistant.commands.CommandProcessor
import com.voiceassistant.wakeword.AudioWakeWordDetector
import java.util.*

class VoiceAssistantService : Service(), TextToSpeech.OnInitListener {

    private lateinit var wakeWordDetector: AudioWakeWordDetector
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var commandProcessor: CommandProcessor

    private var isListeningForCommand = false
    private var isWakeWordDetectorInitialized = false
    private var wakeWordCallback: ((Boolean) -> Unit)? = null
    private var audioDetectionCallback: ((Boolean) -> Unit)? = null

    private lateinit var handlerThread: HandlerThread
    private lateinit var serviceHandler: Handler

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceAssistantChannel"
        private const val TAG = "VoiceAssistantService"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 101
    }

    // Binder pour exposer le service à l'activité
    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
        
        fun setWakeWordCallback(callback: (Boolean) -> Unit) {
            wakeWordCallback = callback
        }
        
        fun setAudioDetectionCallback(callback: (Boolean) -> Unit) {
            audioDetectionCallback = callback
        }
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        handlerThread = HandlerThread("VoiceAssistantThread")
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)

        textToSpeech = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this, textToSpeech)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (checkAudioPermission()) {
            // Initialize TensorFlow wake word detector
            initializeWakeWordDetector()
            // Initialize SpeechRecognizer on main thread (UI)
            Handler(Looper.getMainLooper()).post {
                initializeSpeechRecognizer()
            }
        } else {
            Log.e(TAG, "Audio permissions missing")
            speakText("Audio permissions required")
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice assistant running in background"
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant Active")
            .setContentText("Say 'Hi Aura' to activate")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun initializeWakeWordDetector() {
        try {
            Log.d(TAG, "Initializing wake word detector...")
            wakeWordDetector = AudioWakeWordDetector(
                context = this,
                onWakeWordDetected = {
                    // Wake word detected callback
                    Log.d(TAG, "Wake word detected in service!")
                    onWakeWordDetected()
                },
                onAudioDetected = { detected ->
                    // Audio detection callback
                    Log.d(TAG, "Audio detection callback: $detected")
                    audioDetectionCallback?.invoke(detected)
                }
            )
            
            wakeWordDetector.initialize()
            wakeWordDetector.startListening()
            isWakeWordDetectorInitialized = true
            Log.d(TAG, "Wake word detector initialized and started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize wake word detector: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun onWakeWordDetected() {
        Log.d(TAG, "Wake word detected!")
        
        // Notify MainActivity about wake word detection
        Handler(Looper.getMainLooper()).post {
            wakeWordCallback?.invoke(true)
        }
        
        if (!isListeningForCommand) {
            isListeningForCommand = true
            speakText("Yes, I'm listening")
            startListeningForCommand()
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                isListeningForCommand = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Speech recognition error: $errorMsg")
                speakText("Error: $errorMsg")
                
                // Restart wake word detection after error
                restartWakeWordDetection()
            }

            override fun onResults(results: Bundle?) {
                isListeningForCommand = false
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val command = matches[0]
                        Log.d(TAG, "Recognized command: $command")
                        commandProcessor.processCommand(command)
                    }
                }
                // Restart wake word detection after processing
                restartWakeWordDetection()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun restartWakeWordDetection() {
        if (isWakeWordDetectorInitialized) {
            try {
                wakeWordDetector.startListening()
                Log.d(TAG, "Wake word detection restarted")
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting wake word detection: ${e.message}")
            }
        }
    }

    private fun startListeningForCommand() {
        if (!isWakeWordDetectorInitialized) {
            Log.e(TAG, "Wake word detector not initialized")
            return
        }

        try {
            // Stop wake word detection temporarily to listen for command
            wakeWordDetector.stopListening()
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                }
                speechRecognizer.startListening(intent)
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            restartWakeWordDetection()
        }
    }

    fun testSpeechRecognition() {
        if (!isWakeWordDetectorInitialized) {
            speakText("Wake word detector is not initialized")
            return
        }

        if (isListeningForCommand) {
            speakText("Already listening")
            return
        }

        isListeningForCommand = true
        speakText("Testing speech recognition")
        startListeningForCommand()
    }

    fun testWakeWordDetection() {
        if (!isWakeWordDetectorInitialized) {
            speakText("Wake word detector is not initialized")
            return
        }
        
        speakText("Testing wake word detection. Say Hi Aura now")
        Log.d(TAG, "Wake word detection test started")
    }

    fun getWakeWordStatus(): String {
        return if (isWakeWordDetectorInitialized) {
            "Audio wake word detector is initialized and listening"
        } else {
            "Wake word detector is not initialized"
        }
    }

    fun getDetailedStatus(): String {
        val status = StringBuilder()
        status.append("Service Status:\n")
        status.append("- Wake word detector initialized: $isWakeWordDetectorInitialized\n")
        status.append("- Listening for command: $isListeningForCommand\n")
        status.append("- Audio permission: ${checkAudioPermission()}\n")
        status.append("- Wake word: Hi Aura (Audio Detection)\n")
        status.append("- Detector listening: ${wakeWordDetector.isListening()}\n")
        
        return status.toString()
    }

    private fun speakText(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {
                textToSpeech.setSpeechRate(0.9f)
                textToSpeech.setPitch(1.0f)
                Log.d(TAG, "TTS initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")

        try {
            if (::wakeWordDetector.isInitialized) {
                wakeWordDetector.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying wake word detector: ${e.message}")
        }

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        if (::handlerThread.isInitialized) {
            handlerThread.quitSafely()
        }
    }
}

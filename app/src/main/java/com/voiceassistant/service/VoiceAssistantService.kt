package com.voiceassistant.service

import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
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
import java.util.*

class VoiceAssistantService : Service(), TextToSpeech.OnInitListener, PorcupineManagerCallback {

    private lateinit var porcupineManager: PorcupineManager
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var commandProcessor: CommandProcessor

    private var isListeningForCommand = false
    private var isPorcupineInitialized = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceAssistantChannel"
        private const val PORCUPINE_ACCESS_KEY = "RjAS5pYfRnWFF/oRmXAbSQV25wrP8tVzyaz+UUIVVphZDYjgSVuuLg=="
        private const val TAG = "VoiceAssistantService"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 101
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // Initialisation TTS
        textToSpeech = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this, textToSpeech)

        // Création du canal de notification
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Vérification des permissions avant d'initialiser Porcupine
        if (checkAudioPermission()) {
            initializePorcupine()
            initializeSpeechRecognizer()
        } else {
            Log.e(TAG, "Permissions audio manquantes")
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
            .setContentText("Say 'Hey Gemma' to activate")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun initializePorcupine() {
        try {
            // Vérification que le fichier .ppn existe dans assets
            assets.open("hey-gemma_en_android_v3_0_0.ppn").close()

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(PORCUPINE_ACCESS_KEY)
                .setKeywordPath("hey-gemma_en_android_v3_0_0.ppn") // Fichier dans assets/
                .setSensitivity(0.7f) // Sensibilité ajustée
                .build(applicationContext, this)

            porcupineManager.start()
            isPorcupineInitialized = true
            Log.d(TAG, "Porcupine initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Porcupine initialization failed: ${e.message}")
            speakText("Wake word initialization failed")
            isPorcupineInitialized = false
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
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun invoke(keywordIndex: Int) {
        Log.d(TAG, "Wake word detected")
        if (!isListeningForCommand) {
            isListeningForCommand = true
            speakText("Yes, I'm listening")
            startListeningForCommand()
        }
    }

    private fun startListeningForCommand() {
        if (!isPorcupineInitialized) {
            Log.e(TAG, "Porcupine not initialized")
            return
        }

        try {
            porcupineManager.stop() // Arrête temporairement Porcupine pendant la reconnaissance
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
        }
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")

        try {
            if (::porcupineManager.isInitialized && isPorcupineInitialized) {
                porcupineManager.stop()
                porcupineManager.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}
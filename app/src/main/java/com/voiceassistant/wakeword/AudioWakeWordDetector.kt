package com.voiceassistant.wakeword

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioWakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onAudioDetected: ((Boolean) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var detectionThread: Thread? = null
    
    companion object {
        private const val TAG = "AudioWakeWordDetector"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        private const val AUDIO_CHUNK_SIZE = 1024
        private const val DETECTION_THRESHOLD = 0.1f
        private const val ZERO_CROSSING_THRESHOLD = 0.05f
    }
    
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    )

    fun initialize() {
        try {
            Log.d(TAG, "Audio Wake Word Detector initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Audio detector: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (isListening) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isListening = true

            detectionThread = Thread {
                val buffer = FloatArray(AUDIO_CHUNK_SIZE)

                while (isListening) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
                    
                    if (readSize > 0) {
                        // Calculate audio level
                        val rms = calculateRMS(buffer, readSize)
                        
                        // Simple wake word detection based on audio level and pattern
                        if (detectWakeWordPattern(buffer, readSize)) {
                            Log.d(TAG, "Wake word detected!")
                            onWakeWordDetected()
                            // Add a small delay to prevent multiple detections
                            Thread.sleep(2000)
                        }
                    }
                }
            }
            
            detectionThread?.start()
            Log.d(TAG, "Started listening for wake word")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word detection: ${e.message}")
        }
    }

    fun stopListening() {
        isListening = false
        detectionThread?.interrupt()
        detectionThread = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording: ${e.message}")
        }
        
        Log.d(TAG, "Stopped listening for wake word")
    }

    private fun calculateRMS(buffer: FloatArray, size: Int): Float {
        var sum = 0.0f
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / size)
    }

    private fun detectWakeWordPattern(buffer: FloatArray, size: Int): Boolean {
        // Simple pattern detection for "Hi Aura"
        // This detects speech-like patterns based on audio characteristics
        
        val rms = calculateRMS(buffer, size)
        
        // Check for significant audio activity
        if (rms > DETECTION_THRESHOLD) {
            // Look for a pattern that might indicate speech
            var zeroCrossings = 0
            for (i in 1 until size) {
                if ((buffer[i] >= 0 && buffer[i - 1] < 0) || 
                    (buffer[i] < 0 && buffer[i - 1] >= 0)) {
                    zeroCrossings++
                }
            }
            
            // Speech typically has many zero crossings
            val zeroCrossingRate = zeroCrossings.toFloat() / size
            
            // Check for speech-like characteristics
            val isSpeechLike = zeroCrossingRate > ZERO_CROSSING_THRESHOLD && rms > 0.05f
            
            // Log audio levels for debugging
            if (rms > 0.05f) {
                Log.d(TAG, "Audio detected - RMS: ${String.format("%.4f", rms)}, Zero crossings: ${String.format("%.4f", zeroCrossingRate)}")
                onAudioDetected?.invoke(true)
            } else {
                onAudioDetected?.invoke(false)
            }
            
            if (isSpeechLike) {
                Log.d(TAG, "🎤 SPEECH DETECTED! - RMS: ${String.format("%.4f", rms)}, Zero crossings: ${String.format("%.4f", zeroCrossingRate)}")
            }
            
            return isSpeechLike
        }
        
        return false
    }

    fun isListening(): Boolean = isListening

    fun destroy() {
        stopListening()
    }
} 
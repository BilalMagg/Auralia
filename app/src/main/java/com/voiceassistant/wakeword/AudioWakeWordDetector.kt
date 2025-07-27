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
        private const val DETECTION_THRESHOLD = 0.02f  // Lowered from 0.05f
        private const val ZERO_CROSSING_THRESHOLD = 0.1f
    }
    
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    )

    fun initialize() {
        try {
            Log.d(TAG, "Initializing AudioWakeWordDetector...")
            Log.d(TAG, "Buffer size: $BUFFER_SIZE")
            Log.d(TAG, "Detection threshold: $DETECTION_THRESHOLD")
            Log.d(TAG, "Zero crossing threshold: $ZERO_CROSSING_THRESHOLD")
            Log.d(TAG, "Audio Wake Word Detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Audio detector: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening, skipping start")
            return
        }
        
        try {
            Log.d(TAG, "Starting simple audio detection...")
            
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

            Log.d(TAG, "AudioRecord initialized successfully")
            audioRecord?.startRecording()
            isListening = true

            detectionThread = Thread {
                val buffer = FloatArray(AUDIO_CHUNK_SIZE)
                var silenceFrames = 0
                var speechFrames = 0

                Log.d(TAG, "Simple detection thread started")
                
                while (isListening) {
                    try {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
                        
                        if (readSize > 0) {
                            val rms = calculateRMS(buffer, readSize)
                            
                            // Very simple detection: any significant audio = wake word
                            if (rms > 0.005f) {  // Low threshold to detect any speech
                                speechFrames++
                                silenceFrames = 0
                                onAudioDetected?.invoke(true)
                                
                                Log.d(TAG, "Speech detected! RMS: ${String.format("%.6f", rms)}")
                                
                                // If we have enough consecutive speech frames, trigger wake word
                                if (speechFrames >= 5) {
                                    Log.d(TAG, "🎉 WAKE WORD TRIGGERED! (Speech detected)")
                                    onWakeWordDetected()
                                    speechFrames = 0
                                    Thread.sleep(3000) // Wait 3 seconds before next detection
                                }
                            } else {
                                silenceFrames++
                                speechFrames = 0
                                
                                if (silenceFrames > 20) {
                                    onAudioDetected?.invoke(false)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading audio: ${e.message}")
                        break
                    }
                }
                
                Log.d(TAG, "Detection thread ended")
            }
            
            detectionThread?.start()
            Log.d(TAG, "Simple wake word detection started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting detection: ${e.message}")
            e.printStackTrace()
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
        // Simplified wake word detection - just check for significant audio
        val rms = calculateRMS(buffer, size)
        
        // Log the detection attempt
        Log.d(TAG, "Checking wake word pattern - RMS: ${String.format("%.6f", rms)}")
        
        // Simple threshold-based detection
        return rms > 0.01f
    }

    fun isListening(): Boolean = isListening

    fun destroy() {
        stopListening()
    }
} 
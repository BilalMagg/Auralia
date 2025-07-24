// Hada dyal Bilal

package com.voiceassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.speech.tts.TextToSpeech
import java.util.*

class VoiceAssistantAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {
    
    private lateinit var textToSpeech: TextToSpeech
    
    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    handleNotification(accessibilityEvent)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowChange(accessibilityEvent)
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    handleViewFocus(accessibilityEvent)
                }
            }
        }
    }
    
    private fun handleNotification(event: AccessibilityEvent) {
        val notification = event.text?.joinToString(" ") ?: return
        if (notification.isNotEmpty()) {
            speakText("New notification: $notification")
        }
    }
    
    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return
        
        // Announce app changes for better navigation
        if (packageName != "com.voiceassistant") {
            val appName = getAppName(packageName)
            speakText("Opened $appName")
        }
    }
    
    private fun handleViewFocus(event: AccessibilityEvent) {
        val focusedText = event.text?.joinToString(" ")
        if (!focusedText.isNullOrEmpty()) {
            speakText(focusedText)
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    override fun onInterrupt() {
        // Handle interruption
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            textToSpeech.setSpeechRate(0.9f) // Slightly slower for accessibility
        }
    }
    
    private fun speakText(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null)
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

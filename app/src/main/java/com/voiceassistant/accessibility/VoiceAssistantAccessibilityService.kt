package com.voiceassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceAssistantAccessibilityService : AccessibilityService() {

    // Mock automation state
    private var isAutomationRunning = false
    private var currentTask = ""

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("VAAS", "🎯 SERVICE CONNECTED: Accessibility service is now active!")
        Log.d("VAAS", "🎯 SERVICE CONNECTED: Ready to monitor all apps!")
        
        // Run a simple test immediately to prove the service is working
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("VAAS", "🎯 AUTO TEST: Running automatic test...")
            testSimpleClick()
        }, 2000) // Wait 2 seconds after service starts
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This is where you will handle events and automate UI
        event?.let {
            Log.d("VAAS", "🎯 EVENT: Type=${event.eventType} from ${event.packageName}")
            
            // Check if we're running a mock automation task
            if (isAutomationRunning) {
                handleMockAutomation(event)
            }
        }
    }

    // Mock automation handler
    private fun handleMockAutomation(event: AccessibilityEvent) {
        when (currentTask) {
            "open_alarm_app" -> handleOpenAlarmApp(event)
            "find_alarms" -> handleFindAlarms(event)
            "read_alarm_time" -> handleReadAlarmTime(event)
            else -> {
                Log.d("VAAS", "No active mock task")
            }
        }
    }

    override fun onInterrupt() {
        Log.d("VAAS", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("VAAS", "🎯 SERVICE DESTROYED: Accessibility service stopped")
    }

    // Example: Find node by text
    private fun findNodeByText(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        root ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    // Example: Perform click on a node
    private fun performClick(node: AccessibilityNodeInfo?) {
        node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    // Example: Perform scroll on a node
    private fun performScroll(node: AccessibilityNodeInfo?) {
        node?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    // Example: Perform long click on a node
    private fun performLongClick(node: AccessibilityNodeInfo?) {
        node?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    // Example: Take screenshot (Android 13+)
    private fun takeScreenshotIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            takeScreenshot(
                1, // source
                mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        Log.d("VAAS", "Screenshot taken successfully")
                        // TODO: send screenshot to model (mock for now)
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e("VAAS", "Screenshot failed with error code: $errorCode")
                    }
                }
            )
        } else {
            Log.d("VAAS", "Screenshot not supported on this Android version")
        }
    }

    // Example: Perform gesture (e.g., swipe)
    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    // ===== MOCK AUTOMATION METHODS =====

    // Test function to start mock alarm app automation
    fun startMockAlarmAutomation() {
        Log.d("VAAS", "Starting mock alarm automation")
        isAutomationRunning = true
        currentTask = "open_alarm_app"
        
        // Simulate opening the alarm app
        val intent = packageManager.getLaunchIntentForPackage("com.android.deskclock")
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        
        Log.d("VAAS", "Mock: Opening alarm app")
    }

    // Handle opening alarm app
    private fun handleOpenAlarmApp(event: AccessibilityEvent) {
        if (event.packageName == "com.android.deskclock" && 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            Log.d("VAAS", "Mock: Alarm app opened, taking screenshot")
            takeScreenshotIfSupported()
            
            // Move to next task
            currentTask = "find_alarms"
            Log.d("VAAS", "Mock: Moving to find_alarms task")
        }
    }

    // Handle finding alarms
    private fun handleFindAlarms(event: AccessibilityEvent) {
        if (event.packageName == "com.android.deskclock") {
            val rootNode = rootInActiveWindow
            val alarmButton = findNodeByText(rootNode, "Alarm")
            val alarmTab = findNodeByText(rootNode, "Alarms")
            
            if (alarmButton != null || alarmTab != null) {
                Log.d("VAAS", "Mock: Found alarm button/tab, clicking it")
                performClick(alarmButton ?: alarmTab)
                takeScreenshotIfSupported()
                
                currentTask = "read_alarm_time"
                Log.d("VAAS", "Mock: Moving to read_alarm_time task")
            }
        }
    }

    // Handle reading alarm time
    private fun handleReadAlarmTime(event: AccessibilityEvent) {
        if (event.packageName == "com.android.deskclock") {
            val rootNode = rootInActiveWindow
            // Look for time elements (this is a simplified example)
            val timeElements = rootNode?.findAccessibilityNodeInfosByText(":")
            
            if (timeElements?.isNotEmpty() == true) {
                Log.d("VAAS", "Mock: Found time elements, taking final screenshot")
                takeScreenshotIfSupported()
                
                // Complete the automation
                isAutomationRunning = false
                currentTask = ""
                Log.d("VAAS", "Mock: Alarm automation completed successfully!")
            }
        }
    }

    // Simple test function - just log that it's working
    fun testSimpleClick() {
        Log.d("VAAS", "🎯 SIMPLE TEST: Button clicked!")
        Log.d("VAAS", "🎯 SIMPLE TEST: Accessibility service is working!")
        
        // Just take a screenshot to prove it works
        takeScreenshotIfSupported()
        
        Log.d("VAAS", "🎯 SIMPLE TEST: Test completed successfully!")
    }

    // Test function to simulate scrolling
    fun testScroll() {
        Log.d("VAAS", "Testing scroll automation")
        val rootNode = rootInActiveWindow
        
        // Try to find a scrollable element
        if (rootNode != null) {
            Log.d("VAAS", "Mock: Performing scroll")
            performScroll(rootNode)
            takeScreenshotIfSupported()
        }
    }

    // Test function to simulate swipe gesture
    fun testSwipe() {
        Log.d("VAAS", "Testing swipe gesture")
        // Swipe from center to right
        performSwipe(500f, 1000f, 100f, 1000f)
        Log.d("VAAS", "Mock: Swipe gesture performed")
    }
}
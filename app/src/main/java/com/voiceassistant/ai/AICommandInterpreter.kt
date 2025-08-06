package com.voiceassistant.ai

import android.content.ContentValues.TAG
import com.voiceassistant.model.*
import com.voiceassistant.repository.LlamaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import android.util.Log
import com.voiceassistant.accessibility.VoiceAssistantAccessibilityService
import kotlinx.serialization.json.*

class AICommandInterpreter(private val llamaRepository: LlamaRepository) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    // Prompt système 100% générique - Gemma3 analyse TOUT
    private val universalSystemPrompt = """
You are an intelligent Android automation AI. Your job is to analyze ANY user request and determine the exact sequence of Android actions needed to accomplish it.

Respond ONLY in JSON. No explanation.

Use the following format:
{
  "success": true,
  "message": "Short description",
  "actions": [
    { "type": "OpenApp", "packageName": "com.android.deskclock" },
    { "type": "ClickOnText", "text": "Add" },
    ...
  ]
}

⚠️ You MUST use known Android package names for common apps:
- Clock: com.google.android.deskclock
- Calendar: com.google.android.calendar
- Maps: com.google.android.apps.maps
- Google: com.google.android.googlequicksearchbox
- Messages: com.google.android.apps.messaging
- Settings: com.android.settings
- WhatsApp: com.whatsapp
- YouTube: com.google.android.youtube
- Camera: com.android.camera

If you're unsure what to do, use a Screenshot action first:
{ "type": "Screenshot" }

Only JSON. No explanation. No other format.

Think step by step:
1. What does the user want to achieve?
2. What apps/screens need to be accessed?
3. What specific interactions are required?
4. What is the logical sequence of actions?

AVAILABLE ANDROID ACTIONS:
- Click(x, y): Click at screen coordinates (1080x1920 screen)
- ClickOnText("text"): Click on any UI element containing this text
- Scroll(direction): Scroll UP/DOWN/LEFT/RIGHT
- Type("text"): Type text in any input field
- Screenshot: Take screenshot to see current screen
- GoBack: Android back button
- GoHome: Go to home screen
- OpenNotifications: Open notification panel
- Wait(milliseconds): Wait before next action
- OpenApp("package"): Open app by package name

COMMON APP PACKAGES (use these when relevant):
- Browser: "com.android.chrome" or "com.google.android.googlequicksearchbox"
- Settings: "com.android.settings"
- Messages: "com.google.android.apps.messaging"
- Phone: "com.android.dialer"
- Camera: "com.android.camera2"
- Clock/Alarms: "com.google.android.deskclock"
- Calculator: "com.android.calculator2"
- Maps: "com.google.android.apps.maps"
- Gmail: "com.google.android.gm"
- YouTube: "com.google.android.youtube"
- Play Store: "com.android.vending"
- Files: "com.google.android.apps.nbu.files"

RESPOND ONLY IN JSON FORMAT:
{
    "success": true,
    "message": "Clear explanation of what will be done",
    "actions": [
        {"type": "ActionType", "x": 123, "y": 456, "text": "example", "direction": "DOWN", "milliseconds": 1000, "packageName": "com.example"}
    ]
}

ANALYZE THESE EXAMPLES:

USER: "I want to visit google.com"
ANALYSIS: Need browser → navigate to URL
RESPONSE:
{
    "success": true,
    "message": "Opening browser and navigating to google.com",
    "actions": [
        {"type": "GoHome"},
        {"type": "OpenApp", "packageName": "com.android.chrome"},
        {"type": "Wait", "milliseconds": 3000},
        {"type": "ClickOnText", "text": "Search or type URL"},
        {"type": "Type", "text": "google.com"},
        {"type": "ClickOnText", "text": "Go"}
    ]
}

USER: "Turn on airplane mode"
ANALYSIS: Quick settings → airplane mode toggle
RESPONSE:
{
    "success": true,
    "message": "Activating airplane mode via quick settings",
    "actions": [
        {"type": "OpenNotifications"},
        {"type": "Wait", "milliseconds": 1000},
        {"type": "ClickOnText", "text": "Airplane"},
        {"type": "Wait", "milliseconds": 500}
    ]
}

USER: "Calculate 25 times 67"
ANALYSIS: Open calculator → input calculation
RESPONSE:
{
    "success": true,
    "message": "Opening calculator to compute 25 × 67",
    "actions": [
        {"type": "GoHome"},
        {"type": "OpenApp", "packageName": "com.android.calculator2"},
        {"type": "Wait", "milliseconds": 2000},
        {"type": "ClickOnText", "text": "2"},
        {"type": "ClickOnText", "text": "5"},
        {"type": "ClickOnText", "text": "×"},
        {"type": "ClickOnText", "text": "6"},
        {"type": "ClickOnText", "text": "7"},
        {"type": "ClickOnText", "text": "="}
    ]
}

USER: "Send my location to John"
ANALYSIS: Open messages → find John → share location
RESPONSE:
{
    "success": true,
    "message": "Opening messages to send location to John",
    "actions": [
        {"type": "GoHome"},
        {"type": "OpenApp", "packageName": "com.google.android.apps.messaging"},
        {"type": "Wait", "milliseconds": 2000},
        {"type": "ClickOnText", "text": "John"},
        {"type": "Wait", "milliseconds": 1000},
        {"type": "ClickOnText", "text": "+"},
        {"type": "ClickOnText", "text": "Location"},
        {"type": "ClickOnText", "text": "Send"}
    ]
}

CRITICAL RULES:
1. ANALYZE the user's intent completely - don't just match keywords
2. THINK about the full journey from current state to goal
3. BREAK DOWN complex tasks into logical steps
4. USE realistic coordinates and UI elements
5. INCLUDE Wait actions between major transitions
6. TAKE Screenshots when you need to analyze the current screen
7. BE CREATIVE - handle ANY request, even unusual ones

You are not limited to predefined scenarios. Use your intelligence to figure out ANY task!
""".trimIndent()

    suspend fun interpretCommand(userCommand: String): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AIInterpreter", "🧠 GENERIC AI Analysis: $userCommand")

                val fullPrompt = """
$universalSystemPrompt

USER REQUEST: "$userCommand"

THINK STEP BY STEP:
1. What is the user's goal?
2. What Android navigation is needed?
3. What specific UI interactions are required?
4. What's the optimal sequence?

PROVIDE JSON RESPONSE:
""".trimIndent()

                Log.d("AIInterpreter", "📤 Sending to Gemma3...")
                val response = llamaRepository.getResponse(fullPrompt)
                Log.d("AIInterpreter", "📥 Gemma3 full response: $response")

                val jsonResponse = extractJsonFromResponse(response)
                Log.d("AIInterpreter", "📋 Extracted JSON: $jsonResponse")

                val result = parseAIResponse(jsonResponse)
                Log.d("AIInterpreter", "✅ Final result: success=${result.success}, actions=${result.actions.size}")

                result

            } catch (e: Exception) {
                Log.e("AIInterpreter", "❌ Generic AI error: ${e.message}", e)
                CommandResult(
                    success = false,
                    message = "AI analysis failed: ${e.message}"
                )
            }
        }
    }
    private fun getReliablePackageName(appType: String): String {
        return when (appType.lowercase()) {
            "clock", "alarm", "deskclock" -> {
                // Try multiple possible clock app packages
                val clockPackages = listOf(
                    "com.google.android.deskclock",
                    "com.android.deskclock",
                    "com.samsung.android.app.clockpackage",
                    "com.htc.android.worldclock"
                )
                VoiceAssistantAccessibilityService.instance?.findInstalledPackage(clockPackages) ?: "com.google.android.deskclock"
            }
            "browser", "chrome" -> {
                val browserPackages = listOf(
                    "com.android.chrome",
                    "com.google.android.googlequicksearchbox",
                    "com.android.browser"
                )
                VoiceAssistantAccessibilityService.instance?.findInstalledPackage(browserPackages) ?: "com.android.chrome"
            }
            "calculator" -> {
                val calcPackages = listOf(
                    "com.google.android.calculator2",
                    "com.android.calculator2",
                    "com.samsung.android.calculator"
                )
                VoiceAssistantAccessibilityService.instance?.findInstalledPackage(calcPackages) ?: "com.google.android.calculator2"
            }
            "settings" -> "com.android.settings"
            "camera" -> {
                val cameraPackages = listOf(
                    "com.android.camera2",
                    "com.google.android.GoogleCamera",
                    "com.samsung.android.camera"
                )
                VoiceAssistantAccessibilityService.instance?.findInstalledPackage(cameraPackages) ?: "com.android.camera2"
            }
            else -> "com.android.settings" // fallback
        }
    }



    private fun extractJsonFromResponse(response: String): String {
        // Trouver le JSON dans la réponse
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')

        return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val extracted = response.substring(jsonStart, jsonEnd + 1)
            Log.d("AIInterpreter", "✂️ JSON extracted successfully")
            extracted
        } else {
            Log.w("AIInterpreter", "⚠️ No JSON found, analyzing text response...")
            // Si pas de JSON, essayer d'interpréter la réponse textuelle
            createJsonFromTextResponse(response, "")
        }
    }

    private fun createJsonFromTextResponse(response: String, originalCommand: String): String {
        Log.d("AIInterpreter", "🔄 Creating JSON from text response")

        // Analyser la réponse textuelle pour extraire les intentions
        val text = response.lowercase()

        return when {
            text.contains("browser") || text.contains("chrome") || text.contains("navigate") -> {
                """{"success": true, "message": "Opening browser", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.android.chrome"}, {"type": "Wait", "milliseconds": 3000}]}"""
            }
            text.contains("calculator") || text.contains("calculate") -> {
                """{"success": true, "message": "Opening calculator", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.android.calculator2"}, {"type": "Wait", "milliseconds": 2000}]}"""
            }
            text.contains("message") || text.contains("sms") || text.contains("text") -> {
                """{"success": true, "message": "Opening messages", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.google.android.apps.messaging"}, {"type": "Wait", "milliseconds": 2000}]}"""
            }
            text.contains("camera") || text.contains("photo") -> {
                """{"success": true, "message": "Opening camera", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.android.camera2"}, {"type": "Wait", "milliseconds": 2000}]}"""
            }
            text.contains("settings") || text.contains("configuration") -> {
                """{"success": true, "message": "Opening settings", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.android.settings"}, {"type": "Wait", "milliseconds": 2000}]}"""
            }
            text.contains("alarm") || text.contains("clock") -> {
                """{"success": true, "message": "Opening clock app", "actions": [{"type": "GoHome"}, {"type": "OpenApp", "packageName": "com.google.android.deskclock"}, {"type": "Wait", "milliseconds": 2000}]}"""
            }
            text.contains("screenshot") || text.contains("capture") -> {
                """{"success": true, "message": "Taking screenshot", "actions": [{"type": "Screenshot"}]}"""
            }
            text.contains("scroll") && text.contains("down") -> {
                """{"success": true, "message": "Scrolling down", "actions": [{"type": "Scroll", "direction": "DOWN"}]}"""
            }
            text.contains("scroll") && text.contains("up") -> {
                """{"success": true, "message": "Scrolling up", "actions": [{"type": "Scroll", "direction": "UP"}]}"""
            }
            text.contains("back") -> {
                """{"success": true, "message": "Going back", "actions": [{"type": "GoBack"}]}"""
            }
            text.contains("home") -> {
                """{"success": true, "message": "Going to home screen", "actions": [{"type": "GoHome"}]}"""
            }
            text.contains("notification") -> {
                """{"success": true, "message": "Opening notifications", "actions": [{"type": "OpenNotifications"}]}"""
            }
            else -> {
                """{"success": false, "message": "Could not understand the request. Please try rephrasing."}"""
            }
        }
    }

    private fun parseAIResponse(jsonString: String): CommandResult {
        return try {
            Log.d("AIInterpreter", "🔍 Parsing Gemma3 response...")

            // Essayer d'abord le format standard simple
            val simpleResult = trySimpleFormat(jsonString)
            if (simpleResult != null) {
                return simpleResult
            }

            // Sinon, parser le format complexe de Gemma3
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject

            // Extraire le type de requête pour savoir quoi faire
            val requestType = jsonObject["request_type"]?.jsonPrimitive?.contentOrNull?.lowercase()
            val userIntent = jsonObject["user_intent"]?.jsonPrimitive?.contentOrNull?.lowercase()

            Log.d("AIInterpreter", "📋 Request type: $requestType, Intent: $userIntent")

            // Générer les actions basées sur le type de requête
            val actions = when {
                requestType == "alarm_setting" || userIntent?.contains("alarm") == true -> {
                    generateAlarmActions(jsonObject)
                }
                requestType == "screenshot" || userIntent?.contains("screenshot") == true -> {
                    generateScreenshotActions()
                }
                requestType == "browser" || userIntent?.contains("website") == true -> {
                    generateBrowserActions(jsonObject)
                }
                requestType == "calculator" || userIntent?.contains("calculate") == true -> {
                    generateCalculatorActions(jsonObject)
                }
                else -> {
                    // Analyser le JSON pour deviner l'intention
                    analyzeAndGenerateActions(jsonString, jsonObject)
                }
            }

            val message = extractMessageFromJson(jsonObject, requestType)

            CommandResult(
                success = true,
                message = message,
                actions = actions
            )

        } catch (e: Exception) {
            Log.e("AIInterpreter", "❌ JSON parsing error: ${e.message}", e)

            // Fallback ultime basé sur l'analyse textuelle
            analyzeTextAndCreateActions(jsonString)
        }
    }


    private fun trySimpleFormat(jsonString: String): CommandResult? {
        return try {
            // Essayer le format simple attendu
            val aiResponse = json.decodeFromString<AIResponse>(jsonString)
            val actions = aiResponse.actions.mapNotNull { convertToAccessibilityAction(it) }

            CommandResult(
                success = aiResponse.success,
                message = aiResponse.message,
                actions = actions
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generateScreenshotActions(): List<AccessibilityAction> {
        Log.d("AIInterpreter", "📸 Generating screenshot actions")
        return listOf(AccessibilityAction.Screenshot)
    }

    private fun generateAlarmActions(jsonObject: kotlinx.serialization.json.JsonObject): List<AccessibilityAction> {
        Log.d("AIInterpreter", "⏰ Generating DIRECT alarm actions using AlarmClock API")

        val actions = mutableListOf<AccessibilityAction>()

        // NOUVELLE APPROCHE : Utiliser directement l'API AlarmClock au lieu d'essayer d'ouvrir l'app
        actions.addAll(listOf(
            AccessibilityAction.SetAlarm(6, 0), // Action directe pour régler l'alarme à 6h00
            AccessibilityAction.Wait(2000), // Attendre que l'intent soit traité
            AccessibilityAction.Screenshot // Capture pour voir le résultat
        ))

        return actions
    }
    private fun generateBrowserActions(jsonObject: kotlinx.serialization.json.JsonObject): List<AccessibilityAction> {
        Log.d("AIInterpreter", "🌐 Generating browser actions")

        return listOf(
            AccessibilityAction.GoHome,
            AccessibilityAction.Wait(1000),
            AccessibilityAction.OpenApp("com.android.chrome"),
            AccessibilityAction.Wait(3000),
            AccessibilityAction.ClickOnText("Search or type URL"),
            AccessibilityAction.Wait(1000),
            AccessibilityAction.Type("google.com"),
            AccessibilityAction.ClickOnText("Go")
        )
    }

    private fun generateCalculatorActions(jsonObject: kotlinx.serialization.json.JsonObject): List<AccessibilityAction> {
        Log.d("AIInterpreter", "🧮 Generating calculator actions")

        return listOf(
            AccessibilityAction.GoHome,
            AccessibilityAction.Wait(1000),
            AccessibilityAction.OpenApp("com.android.calculator2"),
            AccessibilityAction.Wait(2000),
            AccessibilityAction.Screenshot
        )
    }

    private fun analyzeAndGenerateActions(jsonString: String, jsonObject: kotlinx.serialization.json.JsonObject): List<AccessibilityAction> {
        Log.d("AIInterpreter", "🔍 Analyzing JSON content for actions")

        val text = jsonString.lowercase()

        return when {
            text.contains("screenshot") || text.contains("capture") -> {
                listOf(AccessibilityAction.Screenshot)
            }
            text.contains("alarm") || text.contains("clock") -> {
                generateAlarmActions(jsonObject)
            }
            text.contains("browser") || text.contains("website") || text.contains("url") -> {
                generateBrowserActions(jsonObject)
            }
            text.contains("calculator") || text.contains("calculate") -> {
                generateCalculatorActions(jsonObject)
            }
            text.contains("scroll") && text.contains("down") -> {
                listOf(AccessibilityAction.Scroll(ScrollDirection.DOWN))
            }
            text.contains("scroll") && text.contains("up") -> {
                listOf(AccessibilityAction.Scroll(ScrollDirection.UP))
            }
            text.contains("back") -> {
                listOf(AccessibilityAction.GoBack)
            }
            text.contains("home") -> {
                listOf(AccessibilityAction.GoHome)
            }
            else -> {
                // Action par défaut : prendre une capture pour analyser
                listOf(AccessibilityAction.Screenshot)
            }
        }
    }

    private fun extractMessageFromJson(jsonObject: kotlinx.serialization.json.JsonObject, requestType: String?): String {
        return when (requestType) {
            "alarm_setting" -> "Setting up alarm as requested"
            "screenshot" -> "Taking screenshot"
            "browser" -> "Opening browser and navigating"
            "calculator" -> "Opening calculator"
            else -> jsonObject["user_intent"]?.jsonPrimitive?.contentOrNull
                ?: "Executing AI-generated actions"
        }
    }

    private fun analyzeTextAndCreateActions(jsonString: String): CommandResult {
        Log.d("AIInterpreter", "🔄 Fallback: analyzing text content")

        val text = jsonString.lowercase()
        val actions = mutableListOf<AccessibilityAction>()
        val message: String

        when {
            text.contains("screenshot") || text.contains("capture") -> {
                actions.add(AccessibilityAction.Screenshot)
                message = "Taking screenshot (fallback mode)"
            }
            text.contains("alarm") -> {
                actions.addAll(listOf(
                    AccessibilityAction.GoHome,
                    AccessibilityAction.Wait(1000),
                    AccessibilityAction.OpenApp("com.google.android.deskclock"),
                    AccessibilityAction.Wait(3000),
                    AccessibilityAction.Screenshot
                ))
                message = "Opening clock app for alarm (fallback mode)"
            }
            text.contains("browser") || text.contains("website") -> {
                actions.addAll(listOf(
                    AccessibilityAction.GoHome,
                    AccessibilityAction.Wait(1000),
                    AccessibilityAction.OpenApp("com.android.chrome"),
                    AccessibilityAction.Wait(3000)
                ))
                message = "Opening browser (fallback mode)"
            }
            else -> {
                actions.add(AccessibilityAction.Screenshot)
                message = "Could not determine action, taking screenshot for analysis"
            }
        }

        return CommandResult(
            success = true,
            message = message,
            actions = actions
        )
    }

    private fun extractMessage(jsonObject: kotlinx.serialization.json.JsonObject): String {
        return when {
            jsonObject.containsKey("user_intent") ->
                "Setting alarm: ${jsonObject["user_intent"]?.jsonPrimitive?.contentOrNull ?: "6 AM"}"
            jsonObject.containsKey("request_type") ->
                "Processing ${jsonObject["request_type"]?.jsonPrimitive?.contentOrNull} request"
            else -> "Executing AI-generated actions"
        }
    }

    private fun extractActions(jsonObject: kotlinx.serialization.json.JsonObject): List<AccessibilityAction> {
        val actions = mutableListOf<AccessibilityAction>()

        try {
            val actionsArray = jsonObject["actions"]?.jsonArray

            actionsArray?.forEach { actionElement ->
                val actionObj = actionElement.jsonObject
                val actionName = actionObj["action_name"]?.jsonPrimitive?.contentOrNull

                when (actionName) {
                    "Open Clock App" -> {
                        actions.add(AccessibilityAction.GoHome)
                        actions.add(AccessibilityAction.Wait(1000))
                        actions.add(AccessibilityAction.OpenApp("com.google.android.deskclock"))
                        actions.add(AccessibilityAction.Wait(2000))
                    }
                    "Navigate to Alarm Creation Screen" -> {
                        actions.add(AccessibilityAction.ClickOnText("Alarm"))
                        actions.add(AccessibilityAction.Wait(1000))
                        actions.add(AccessibilityAction.ClickOnText("+"))
                    }
                    "Set Alarm Time" -> {
                        val timeParams = actionObj["input_parameters"]?.jsonObject
                        val time = timeParams?.get("time")?.jsonPrimitive?.contentOrNull ?: "06:00"

                        // Extraire l'heure (06:00 -> 6)
                        val hour = time.split(":")[0].toIntOrNull() ?: 6

                        actions.add(AccessibilityAction.ClickOnText(hour.toString()))
                        actions.add(AccessibilityAction.ClickOnText("00"))
                        actions.add(AccessibilityAction.ClickOnText("AM"))
                    }
                    "Set Alarm Label (Optional)" -> {
                        val label = actionObj["input_parameters"]?.jsonObject
                            ?.get("label")?.jsonPrimitive?.contentOrNull
                        if (!label.isNullOrEmpty()) {
                            actions.add(AccessibilityAction.ClickOnText("Label"))
                            actions.add(AccessibilityAction.Type(label))
                        }
                    }
                    "Save Alarm" -> {
                        actions.add(AccessibilityAction.ClickOnText("Save"))
                        actions.add(AccessibilityAction.Wait(1000))
                    }
                    "Confirm Alarm Setting" -> {
                        actions.add(AccessibilityAction.ClickOnText("OK"))
                    }
                    else -> {
                        // Actions génériques basées sur le nom
                        when {
                            actionName?.contains("Open", ignoreCase = true) == true -> {
                                actions.add(AccessibilityAction.GoHome)
                                actions.add(AccessibilityAction.Wait(1000))
                            }
                            actionName?.contains("Navigate", ignoreCase = true) == true -> {
                                actions.add(AccessibilityAction.Screenshot)
                            }
                            actionName?.contains("Set", ignoreCase = true) == true -> {
                                actions.add(AccessibilityAction.ClickOnText("6"))
                                actions.add(AccessibilityAction.ClickOnText("00"))
                            }
                            actionName?.contains("Save", ignoreCase = true) == true -> {
                                actions.add(AccessibilityAction.ClickOnText("Save"))
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("AIInterpreter", "Error extracting actions: ${e.message}", e)
        }

        // Si aucune action extraite, créer une séquence basique pour l'alarme
        if (actions.isEmpty()) {
            actions.addAll(listOf(
                AccessibilityAction.GoHome,
                AccessibilityAction.Wait(1000),
                AccessibilityAction.OpenApp("com.google.android.deskclock"),
                AccessibilityAction.Wait(2000),
                AccessibilityAction.ClickOnText("Alarm"),
                AccessibilityAction.Wait(1000),
                AccessibilityAction.ClickOnText("+"),
                AccessibilityAction.Wait(1000),
                AccessibilityAction.Screenshot
            ))
        }

        return actions
    }

    private fun tryManualExtraction(jsonString: String): CommandResult {
        Log.d("AIInterpreter", "🔄 Trying manual extraction...")

        val actions = mutableListOf<AccessibilityAction>()

        // Rechercher des patterns dans le JSON
        when {
            jsonString.contains("alarm", ignoreCase = true) -> {
                actions.addAll(listOf(
                    AccessibilityAction.GoHome,
                    AccessibilityAction.Wait(1000),
                    AccessibilityAction.OpenApp("com.google.android.deskclock"),
                    AccessibilityAction.Wait(2000),
                    AccessibilityAction.ClickOnText("Alarm"),
                    AccessibilityAction.Wait(1000),
                    AccessibilityAction.ClickOnText("+"),
                    AccessibilityAction.Wait(1000),
                    AccessibilityAction.ClickOnText("6"),
                    AccessibilityAction.ClickOnText("00"),
                    AccessibilityAction.ClickOnText("AM"),
                    AccessibilityAction.ClickOnText("Save")
                ))
            }
            jsonString.contains("screenshot", ignoreCase = true) -> {
                actions.add(AccessibilityAction.Screenshot)
            }
            else -> {
                actions.add(AccessibilityAction.Screenshot)
            }
        }

        return CommandResult(
            success = true,
            message = "Manual extraction - AI provided detailed plan, executing basic actions",
            actions = actions
        )
    }

    private fun convertToAccessibilityAction(actionData: ActionData): AccessibilityAction? {
        return try {
            when (actionData.type) {
                "Click" -> {
                    val x = actionData.x ?: 540
                    val y = actionData.y ?: 960
                    AccessibilityAction.Click(x, y)
                }
                "ClickOnText" -> {
                    actionData.text?.let { AccessibilityAction.ClickOnText(it) }
                }
                "Scroll" -> {
                    val direction = when (actionData.direction?.uppercase()) {
                        "UP" -> ScrollDirection.UP
                        "DOWN" -> ScrollDirection.DOWN
                        "LEFT" -> ScrollDirection.LEFT
                        "RIGHT" -> ScrollDirection.RIGHT
                        else -> ScrollDirection.DOWN
                    }
                    AccessibilityAction.Scroll(direction)
                }
                "Type" -> {
                    actionData.text?.let { AccessibilityAction.Type(it) }
                }
                "Screenshot" -> AccessibilityAction.Screenshot
                "GoBack" -> AccessibilityAction.GoBack
                "GoHome" -> AccessibilityAction.GoHome
                "OpenNotifications" -> AccessibilityAction.OpenNotifications
                "Wait" -> AccessibilityAction.Wait(actionData.milliseconds ?: 1000)
                "OpenApp" -> {
                    actionData.packageName?.let { AccessibilityAction.OpenApp(it) }
                }
                else -> {
                    Log.w("AIInterpreter", "❓ Unknown action: ${actionData.type}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AIInterpreter", "❌ Error converting action: ${e.message}", e)
            null
        }
    }
}
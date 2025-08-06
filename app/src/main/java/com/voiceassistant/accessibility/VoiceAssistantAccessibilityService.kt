package com.voiceassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ContentValues
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.*
import kotlinx.coroutines.*
import com.voiceassistant.model.*
import android.provider.AlarmClock
import kotlinx.coroutines.*
class VoiceAssistantAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        var instance: VoiceAssistantAccessibilityService? = null
        private const val TAG = "AutoclickService"
    }

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        Log.d(TAG, "Service d'accessibilité créé")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Remettre à jour le serviceInfo en code, pour être sûr
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info

        Log.d(TAG, "Service connecté avec nouvelle config serviceInfo")
    }

    private suspend fun openAppAndAwaitWindow(pkg: String): Boolean {
        if (!performOpenApp(pkg)) return false

        // Poller pendant max 10 s
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 10_000) {
            val root = rootInActiveWindow
            Log.d(TAG, "=== DUMP ROOT ===")
            fun dump(node: AccessibilityNodeInfo, indent: String = "") {
                Log.d(TAG, "$indent ${node.className}:${node.text}")
                for (i in 0 until node.childCount) node.getChild(i)?.let { dump(it, "$indent  ") }
            }
            dump(root)
            if (root?.packageName?.toString() == pkg) {
                Log.d(TAG, "$pkg détecté à l'écran, childCount=${root.childCount}")
                return true
            }
            delay(300)
        }
        Log.w(TAG, "⏱️ Timeout waiting for $pkg window")
        return false
    }
    private fun openClockOrSetAlarm(hour: Int = 6, minutes: Int = 0): Boolean {
        return try {
            // Lance directement l'écran de création d'alarme
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                // facultatif :
                putExtra(AlarmClock.EXTRA_MESSAGE, "Réveil Auralia")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Impossible d’ouvrir l’horloge via AlarmClock API: ${e.message}", e)
            false
        }
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
            Log.d(TAG, "Notification: $notification")
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.voiceassistant") {
            val appName = getAppName(packageName)
            Log.d(TAG, "App changée: $appName")
        }
    }

    private fun handleViewFocus(event: AccessibilityEvent) {
        val focusedText = event.text?.joinToString(" ")
        if (!focusedText.isNullOrEmpty() && focusedText.length < 30) {
            Log.d(TAG, "Focus: $focusedText")
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
        Log.d(TAG, "Service interrompu")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            textToSpeech.setSpeechRate(0.9f)
            Log.d(TAG, "TextToSpeech initialisé")
        }
    }

    // ==========================================
    // API AUTOCLICK GÉNÉRIQUE
    // ==========================================

    fun executeGenericCommand(command: String): Boolean {
        Log.d(TAG, "🎯 Commande reçue: '$command'")

        val normalizedCommand = command.lowercase().trim()

        return try {
            when {
                // Test simple pour vérifier la connexion
                normalizedCommand.contains("test simple") -> {
                    Log.d(TAG, "Test simple réussi")
                    speakText("Test de connexion réussi")
                    true
                }

                // Clic par coordonnées: "clique à 300,500"
                normalizedCommand.matches(Regex(".*clique?\\s+(?:à|at)\\s+(\\d+)\\s*[,\\s]\\s*(\\d+).*")) -> {
                    handleClickAtCoordinates(normalizedCommand)
                }

                // Clic par texte: "clique sur OK"
                normalizedCommand.matches(Regex(".*clique?\\s+sur\\s+(.+)")) ||
                        normalizedCommand.matches(Regex(".*click\\s+on\\s+(.+)")) -> {
                    handleClickOnText(normalizedCommand)
                }

                // Taper du texte: "tape 'Hello'"
                normalizedCommand.matches(Regex(".*(?:tape|type)\\s+['\"](.+)['\"].*")) -> {
                    handleTypeText(normalizedCommand)
                }

                // Défilement
                normalizedCommand.contains("scroll") || normalizedCommand.contains("défil") -> {
                    handleScroll(normalizedCommand)
                }

                // Navigation système
                normalizedCommand.contains("retour") || normalizedCommand.contains("back") -> {
                    handleGoBack()
                }

                normalizedCommand.contains("accueil") || normalizedCommand.contains("home") -> {
                    handleGoHome()
                }

                // Prendre une capture d'écran
                normalizedCommand.contains("screenshot") || normalizedCommand.contains("capture") -> {
                    handleTakeScreenshot()
                }

                // Ouvrir les notifications
                normalizedCommand.contains("notification") -> {
                    handleOpenNotifications()
                }

                // Commande non reconnue
                else -> {
                    Log.w(TAG, "Commande non reconnue: $command")
                    speakText("Commande non reconnue")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur: ${e.message}", e)
            speakText("Erreur lors de l'exécution")
            false
        }
    }

    // ==========================================
    // GESTIONNAIRES DE COMMANDES
    // ==========================================

    private fun handleClickAtCoordinates(command: String): Boolean {
        val regex = Regex(".*clique?\\s+(?:à|at)\\s+(\\d+)\\s*[,\\s]\\s*(\\d+).*")
        val match = regex.find(command)

        return if (match != null) {
            val x = match.groupValues[1].toFloat()
            val y = match.groupValues[2].toFloat()

            Log.d(TAG, "Clic aux coordonnées: ($x, $y)")
            val success = performClickAtCoordinates(x, y)

            if (success) {
                speakText("Clic effectué à $x, $y")
                Log.d(TAG, "Clic par coordonnées réussi")
            } else {
                speakText("Échec du clic aux coordonnées")
                Log.e(TAG, "Échec du clic par coordonnées")
            }
            success
        } else {
            speakText("Format de coordonnées invalide")
            false
        }
    }

    private fun handleClickOnText(command: String): Boolean {
        val regexFr = Regex(".*clique?\\s+sur\\s+(.+)")
        val regexEn = Regex(".*click\\s+on\\s+(.+)")

        val match = regexFr.find(command) ?: regexEn.find(command)

        return if (match != null) {
            val targetText = match.groupValues[1].trim()
            Log.d(TAG, "Recherche élément avec texte: '$targetText'")

            val success = clickOnTextElement(targetText) ||
                    clickOnDescriptionElement(targetText) ||
                    clickOnPartialTextElement(targetText)

            if (success) {
                speakText("Clic effectué sur $targetText")
                Log.d(TAG, "Clic par texte réussi: $targetText")
            } else {
                speakText("Élément '$targetText' non trouvé")
                Log.w(TAG, "Élément non trouvé: $targetText")
                listAvailableElements()
            }
            success
        } else {
            speakText("Format de commande invalide")
            false
        }
    }

    private fun handleTypeText(command: String): Boolean {
        val regex = Regex(".*(?:tape|type)\\s+['\"](.+)['\"].*")
        val match = regex.find(command)

        return if (match != null) {
            val textToType = match.groupValues[1]
            Log.d(TAG, "Saisie texte: '$textToType'")

            val success = typeTextInField(textToType)

            if (success) {
                speakText("Texte saisi: $textToType")
                Log.d(TAG, "Saisie de texte réussie")
            } else {
                speakText("Aucun champ de saisie trouvé")
                Log.w(TAG, "Aucun champ de saisie disponible")
            }
            success
        } else {
            speakText("Format de texte invalide")
            false
        }
    }

    private fun handleScroll(command: String): Boolean {
        val direction = when {
            command.contains("up") || command.contains("haut") -> "UP"
            command.contains("down") || command.contains("bas") -> "DOWN"
            command.contains("left") || command.contains("gauche") -> "LEFT"
            command.contains("right") || command.contains("droite") -> "RIGHT"
            else -> "DOWN"
        }

        Log.d(TAG, "Défilement vers: $direction")
        val success = performScroll(direction)

        if (success) {
            speakText("Défilement $direction effectué")
            Log.d(TAG, "Défilement réussi: $direction")
        } else {
            speakText("Échec du défilement")
            Log.w(TAG, "Échec du défilement: $direction")
        }

        return success
    }

    private fun handleGoBack(): Boolean {
        Log.d(TAG, "Exécution: Retour système")
        val success = performGlobalAction(GLOBAL_ACTION_BACK)

        if (success) {
            speakText("Retour effectué")
            Log.d(TAG, "Action retour réussie")
        } else {
            speakText("Échec du retour")
            Log.w(TAG, "Échec action retour")
        }

        return success
    }

    private fun handleGoHome(): Boolean {
        Log.d(TAG, "Exécution: Accueil système")
        val success = performGlobalAction(GLOBAL_ACTION_HOME)

        if (success) {
            speakText("Retour à l'accueil")
            Log.d(TAG, "Action accueil réussie")
        } else {
            speakText("Échec retour accueil")
            Log.w(TAG, "Échec action accueil")
        }

        return success
    }

    private fun handleTakeScreenshot(): Boolean {
        Log.d(TAG, "Exécution: Capture d'écran")
        val success = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)

        if (success) {
            speakText("Capture d'écran prise")
            Log.d(TAG, "Capture d'écran réussie")
        } else {
            speakText("Échec de la capture")
            Log.w(TAG, "Échec capture d'écran")
        }

        return success
    }

    private fun handleOpenNotifications(): Boolean {
        Log.d(TAG, "Exécution: Ouverture notifications")
        val success = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

        if (success) {
            speakText("Notifications ouvertes")
            Log.d(TAG, "Ouverture notifications réussie")
        } else {
            speakText("Échec ouverture notifications")
            Log.w(TAG, "Échec ouverture notifications")
        }

        return success
    }

    // ==========================================
    // MÉTHODES D'AUTOCLICK CORE
    // ==========================================

    private fun performClickAtCoordinates(x: Float, y: Float): Boolean {
        return try {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()

            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur clic coordonnées: ${e.message}")
            false
        }
    }

    private fun clickOnTextElement(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByExactText(rootNode, text)

        return targetNode?.let { node ->
            Log.d(TAG, "Élément trouvé par texte exact: '${node.text}'")
            performClickOnNode(node)
        } ?: false
    }

    private fun clickOnDescriptionElement(description: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByContentDescription(rootNode, description)

        return targetNode?.let { node ->
            Log.d(TAG, "Élément trouvé par description: '${node.contentDescription}'")
            performClickOnNode(node)
        } ?: false
    }

    private fun clickOnPartialTextElement(partialText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByPartialText(rootNode, partialText)

        return targetNode?.let { node ->
            Log.d(TAG, "Élément trouvé par texte partiel: '${node.text}'")
            performClickOnNode(node)
        } ?: false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            // Méthode 1: Clic direct via l'action
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.d(TAG, "Clic direct sur nœud réussi")
                return true
            }

            // Méthode 2: Clic par coordonnées du nœud
            val rect = Rect()
            node.getBoundsInScreen(rect)

            if (!rect.isEmpty) {
                val centerX = rect.centerX().toFloat()
                val centerY = rect.centerY().toFloat()
                Log.d(TAG, "Clic par coordonnées nœud: ($centerX, $centerY)")
                return performClickAtCoordinates(centerX, centerY)
            }

            Log.w(TAG, "Impossible de cliquer sur le nœud")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erreur clic sur nœud: ${e.message}")
            false
        }
    }

    private fun typeTextInField(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editText = findEditTextField(rootNode)

        return editText?.let { node ->
            try {
                Log.d(TAG, "Champ de saisie trouvé: ${node.className}")
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur saisie texte: ${e.message}")
                false
            }
        } ?: false
    }

    private fun performScroll(direction: String): Boolean {
        return try {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()

            val path = Path()

            when (direction) {
                "UP" -> {
                    path.moveTo(screenWidth / 2, screenHeight * 0.8f)
                    path.lineTo(screenWidth / 2, screenHeight * 0.2f)
                }
                "DOWN" -> {
                    path.moveTo(screenWidth / 2, screenHeight * 0.2f)
                    path.lineTo(screenWidth / 2, screenHeight * 0.8f)
                }
                "LEFT" -> {
                    path.moveTo(screenWidth * 0.8f, screenHeight / 2)
                    path.lineTo(screenWidth * 0.2f, screenHeight / 2)
                }
                "RIGHT" -> {
                    path.moveTo(screenWidth * 0.2f, screenHeight / 2)
                    path.lineTo(screenWidth * 0.8f, screenHeight / 2)
                }
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()

            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur défilement: ${e.message}")
            false
        }
    }

    // ==========================================
    // UTILITAIRES DE RECHERCHE
    // ==========================================

    private fun findNodeByExactText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        try {
            if (root.text?.toString()?.equals(text, ignoreCase = true) == true) {
                return root
            }

            for (i in 0 until root.childCount) {
                root.getChild(i)?.let { child ->
                    val result = findNodeByExactText(child, text)
                    if (result != null) return result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche texte exact: ${e.message}")
        }
        return null
    }

    private fun findNodeByPartialText(root: AccessibilityNodeInfo, partialText: String): AccessibilityNodeInfo? {
        try {
            if (root.text?.toString()?.contains(partialText, ignoreCase = true) == true) {
                return root
            }

            for (i in 0 until root.childCount) {
                root.getChild(i)?.let { child ->
                    val result = findNodeByPartialText(child, partialText)
                    if (result != null) return result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche texte partiel: ${e.message}")
        }
        return null
    }

    private fun findNodeByContentDescription(root: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        try {
            if (root.contentDescription?.toString()?.contains(description, ignoreCase = true) == true) {
                return root
            }

            for (i in 0 until root.childCount) {
                root.getChild(i)?.let { child ->
                    val result = findNodeByContentDescription(child, description)
                    if (result != null) return result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche description: ${e.message}")
        }
        return null
    }

    private fun findEditTextField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            if (root.className == "android.widget.EditText" ||
                (root.isFocusable && root.isEditable)) {
                return root
            }

            for (i in 0 until root.childCount) {
                root.getChild(i)?.let { child ->
                    val result = findEditTextField(child)
                    if (result != null) return result
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche champ éditable: ${e.message}")
        }
        return null
    }

    // ==========================================
    // UTILITAIRES DE DEBUG
    // ==========================================

    private fun listAvailableElements() {
        try {
            val rootNode = rootInActiveWindow ?: return
            val elements = mutableListOf<String>()

            collectClickableElements(rootNode, elements)

            if (elements.isNotEmpty()) {
                val elementsList = elements.take(5).joinToString(", ")
                Log.d(TAG, "Éléments cliquables disponibles: $elements")
                speakText("Éléments disponibles: $elementsList")
            } else {
                Log.d(TAG, "Aucun élément cliquable trouvé")
                speakText("Aucun élément cliquable visible")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur listing éléments: ${e.message}")
        }
    }

    private fun collectClickableElements(node: AccessibilityNodeInfo, elements: MutableList<String>) {
        try {
            if (node.isClickable) {
                val text = node.text?.toString()
                val description = node.contentDescription?.toString()

                when {
                    !text.isNullOrBlank() -> elements.add("'$text'")
                    !description.isNullOrBlank() -> elements.add("[$description]")
                    else -> elements.add("(Bouton sans texte)")
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectClickableElements(child, elements)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur collection éléments: ${e.message}")
        }
    }

    // ==========================================
    // MÉTHODES UTILITAIRES
    // ==========================================

    private fun speakText(text: String) {
        try {
            if (::textToSpeech.isInitialized) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null)
            }
            Log.d(TAG, "🔊 TTS: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur TTS: ${e.message}")
        }
    }

    /**
     * Fonction principale pour exécuter une séquence d'actions - NOUVELLE MÉTHODE À AJOUTER
     */
    suspend fun executeActionSequence(sequence: ActionSequence): Boolean {
        return withContext(Dispatchers.Main) {
            Log.d(TAG, "🎯 EXECUTING SEQUENCE: ${sequence.description}")
            Log.d(TAG, "📋 Total actions: ${sequence.actions.size}")

            // Debug: Print all actions first
            sequence.actions.forEachIndexed { index, action ->
                Log.d(TAG, "   Action ${index + 1}: $action")
            }

            speakText("Exécution de ${sequence.actions.size} actions...")

            var allSuccess = true
            var actionIndex = 0

            for (action in sequence.actions) {
                actionIndex++
                Log.d(TAG, "🔄 EXECUTING Action $actionIndex/${sequence.actions.size}: $action")

                try {
                    val success = when (action) {
                        is AccessibilityAction.Wait -> {
                            Log.d(TAG, "⏱️ Waiting ${action.milliseconds}ms")
                            delay(action.milliseconds)
                            true
                        }
                        is AccessibilityAction.OpenApp -> {
                            Log.d(TAG, "📱 Opening app: ${action.packageName}")

                            // NOUVEAU: Détection spéciale pour les apps d'alarme
                            if (action.packageName.contains("deskclock") || action.packageName.contains("clock")) {
                                Log.d(TAG, "🚨 Alarm app detected, using AlarmClock API")
                                openClockOrSetAlarm(6, 0) // 6h00 par défaut
                            } else {
                                val result = openAppWithDelay(action.packageName)
                                if (result) {
                                    Log.d(TAG, "✅ App opened, waiting for load...")
                                    delay(4000) // Wait longer for app to fully load
                                } else {
                                    Log.e(TAG, "❌ Failed to open app: ${action.packageName}")
                                }
                                result
                            }
                        }
                        // NOUVEAU: Gestion directe des alarmes
                        is AccessibilityAction.SetAlarm -> {
                            Log.d(TAG, "⏰ Setting alarm directly: ${action.hour}:${action.minute}")
                            openClockOrSetAlarm(action.hour, action.minute)
                        }
                        is AccessibilityAction.GoHome -> {
                            Log.d(TAG, "🏠 Going home")
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                        is AccessibilityAction.Screenshot -> {
                            Log.d(TAG, "📸 Taking screenshot")
                            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                        is AccessibilityAction.ClickOnText -> {
                            Log.d(TAG, "👆 Clicking on text: ${action.text}")
                            delay(1000) // Wait before clicking
                            clickOnTextElement(action.text) ||
                                    clickOnPartialTextElement(action.text) ||
                                    clickOnDescriptionElement(action.text)
                        }
                        else -> {
                            executeActionSync(action)
                        }
                    }

                    if (!success) {
                        allSuccess = false
                        Log.e(TAG, "❌ FAILED Action $actionIndex: $action")
                    } else {
                        Log.d(TAG, "✅ SUCCESS Action $actionIndex: $action")
                    }

                    // Longer delay between actions for stability
                    delay(800)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ EXCEPTION Action $actionIndex: ${e.message}", e)
                    allSuccess = false
                }
            }

            val resultMessage = if (allSuccess) {
                "✅ Toutes les ${sequence.actions.size} actions réussies"
            } else {
                "⚠️ Certaines actions réussies mais app d'alarme peut manquer"
            }

            Log.d(TAG, "🏁 SEQUENCE COMPLETED: $resultMessage")
            speakText(resultMessage)

            allSuccess
        }
    }
    private fun executeActionSuccess(action: AccessibilityAction): Boolean {
        // This is just for counting, actual execution happens in executeActionSequence
        return true
    }
    // Add this synchronous version for actions that don't need delay:
    private fun executeActionSync(action: AccessibilityAction): Boolean {
        return try {
            when (action) {
                is AccessibilityAction.Click -> {
                    Log.d(TAG, "👆 Clic à (${action.x}, ${action.y})")
                    performClickAtCoordinates(action.x.toFloat(), action.y.toFloat())
                }
                is AccessibilityAction.ClickOnText -> {
                    Log.d(TAG, "👆 Clic sur texte: ${action.text}")
                    clickOnTextElement(action.text) ||
                            clickOnPartialTextElement(action.text) ||
                            clickOnDescriptionElement(action.text)
                }
                is AccessibilityAction.Scroll -> {
                    Log.d(TAG, "📜 Défilement: ${action.direction}")
                    performScroll(action.direction.name)
                }
                is AccessibilityAction.Type -> {
                    Log.d(TAG, "⌨️ Saisie: ${action.text}")
                    typeTextInField(action.text)
                }
                is AccessibilityAction.Screenshot -> {
                    Log.d(TAG, "📸 Capture d'écran")
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
                is AccessibilityAction.GoBack -> {
                    Log.d(TAG, "⬅️ Retour")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                is AccessibilityAction.GoHome -> {
                    Log.d(TAG, "🏠 Accueil")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
                is AccessibilityAction.OpenNotifications -> {
                    Log.d(TAG, "🔔 Notifications")
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                }
                // NOUVEAU: Gestion de SetAlarm dans executeActionSync aussi
                is AccessibilityAction.SetAlarm -> {
                    Log.d(TAG, "⏰ Setting alarm: ${action.hour}:${action.minute}")
                    openClockOrSetAlarm(action.hour, action.minute)
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur action sync: ${e.message}", e)
            false
        }
    }

     fun findInstalledPackage(packages: List<String>): String? {
        for (packageName in packages) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                Log.d(ContentValues.TAG, "✅ Found installed package: $packageName")
                return packageName
            } catch (e: Exception) {
                Log.d(ContentValues.TAG, "❌ Package not found: $packageName")
            }
        }
        return null
    }
    private suspend fun openAppWithDelay(packageName: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "🚀 ATTEMPTING TO OPEN: $packageName")

                // Step 1: Check if package exists
                val packageExists = try {
                    packageManager.getPackageInfo(packageName, 0)
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "❌ Package not installed: $packageName")
                    false
                }

                if (!packageExists) {
                    // Try alternative packages
                    val alternatives = getAlternativePackages(packageName)
                    for (altPackage in alternatives) {
                        Log.d(TAG, "🔄 Trying alternative: $altPackage")
                        if (openAppWithDelay(altPackage)) {
                            return@withContext true
                        }
                    }
                    return@withContext false
                }

                // Step 2: Get launch intent
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent == null) {
                    Log.w(TAG, "❌ No launch intent for: $packageName")
                    return@withContext false
                }

                // Step 3: Configure intent properly
                intent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )

                Log.d(TAG, "🎯 Starting activity with intent: $intent")
                Log.d(TAG, "   - Action: ${intent.action}")
                Log.d(TAG, "   - Component: ${intent.component}")
                Log.d(TAG, "   - Flags: ${intent.flags}")

                // Step 4: Start the activity
                startActivity(intent)

                Log.d(TAG, "✅ Activity started successfully: $packageName")
                return@withContext true

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException opening $packageName: ${e.message}")
                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error opening $packageName: ${e.message}", e)
                return@withContext false
            }
        }
    }

    private fun getAlternativePackages(packageName: String): List<String> {
        return when (packageName) {
            "com.google.android.deskclock" -> listOf(
                "com.android.deskclock",
                "com.samsung.android.app.clockpackage",
                "com.htc.android.worldclock",
                "com.oneplus.deskclock"
            )
            "com.android.chrome" -> listOf(
                "com.google.android.googlequicksearchbox",
                "com.android.browser"
            )
            "com.google.android.calculator2" -> listOf(
                "com.android.calculator2",
                "com.samsung.android.calculator"
            )
            else -> emptyList()
        }
    }

    private suspend fun openAppAndWait(packageName: String): Boolean {
        val launched = performOpenApp(packageName)
        if (!launched) return false

        // Attendre que la nouvelle fenêtre de l'app soit active
        val start = System.currentTimeMillis()
        val timeout = 10_000L // 10s max
        while (System.currentTimeMillis() - start < timeout) {
            val root = rootInActiveWindow
            Log.d(TAG, "=== DUMP ROOT ===")
            if (root?.packageName?.toString() == packageName) {
                Log.d(TAG, "🟢 $packageName est à l'écran")
                return true
            }
            delay(300)
        }
        Log.w(TAG, "⚠️ Timeout waiting for $packageName window")
        return false
    }


    /**
     * Exécuter une action individuelle - NOUVELLE MÉTHODE À AJOUTER
     */
//    private suspend fun executeAction(action: AccessibilityAction): Boolean {
//        return try {
//            when (action) {
//                is AccessibilityAction.Click -> {
//                    Log.d(TAG, "👆 Clic à (${action.x}, ${action.y})")
//                    performClickAtCoordinates(action.x.toFloat(), action.y.toFloat())
//                }
//                is AccessibilityAction.ClickOnText -> {
//                    Log.d(TAG, "👆 Clic sur texte: ${action.text}")
//                    clickOnTextElement(action.text)
//                }
//                is AccessibilityAction.Scroll -> {
//                    Log.d(TAG, "📜 Défilement: ${action.direction}")
//                    performScroll(action.direction.name)
//                }
//                is AccessibilityAction.Type -> {
//                    Log.d(TAG, "⌨️ Saisie: ${action.text}")
//                    typeTextInField(action.text)
//                }
//                is AccessibilityAction.Screenshot -> {
//                    Log.d(TAG, "📸 Capture d'écran")
//                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
//                }
//                is AccessibilityAction.GoBack -> {
//                    Log.d(TAG, "⬅️ Retour")
//                    performGlobalAction(GLOBAL_ACTION_BACK)
//                }
//                is AccessibilityAction.GoHome -> {
//                    Log.d(TAG, "🏠 Accueil")
//                    performGlobalAction(GLOBAL_ACTION_HOME)
//                }
//                is AccessibilityAction.OpenNotifications -> {
//                    Log.d(TAG, "🔔 Notifications")
//                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
//                }
//                is AccessibilityAction.Wait -> {
//                    Log.d(TAG, "⏱️ Attendre ${action.milliseconds}ms")
//                    delay(action.milliseconds)
//                    true
//                }
//                is AccessibilityAction.OpenApp -> {
//                    Log.d(TAG, "📱 Ouvrir app: ${action.packageName}")
//                    val success = performOpenApp(action.packageName)
//                    if (success) {
//                        // Wait for app to load
//                        delay(3000)
//                    }
//                    success
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Erreur action $action: ${e.message}", e)
//            false
//        }
//    }
    /**
     * Ouvrir une application - NOUVELLE MÉTHODE À AJOUTER
     */
    private fun performOpenApp(packageName: String): Boolean {
        return try {
            Log.d(TAG, "🚀 Attempting to open: $packageName")

            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                Log.d(TAG, "✅ App launched via intent: $packageName")
                return true
            } else {
                Log.w(TAG, "⚠️ No launch intent for $packageName")

                // Try alternative package names for common apps
                val alternativePackage = getAlternativePackage(packageName)
                if (alternativePackage != null && alternativePackage != packageName) {
                    Log.d(TAG, "🔄 Trying alternative package: $alternativePackage")
                    return performOpenApp(alternativePackage)
                }

                // Final fallback: go home and try to find the app
                performGlobalAction(GLOBAL_ACTION_HOME)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening app: ${e.message}", e)
            false
        }
    }

    private fun getAlternativePackage(packageName: String): String? {
        return when (packageName) {
            "com.google.android.deskclock" -> "com.android.deskclock"
            "com.android.chrome" -> "com.google.android.googlequicksearchbox"
            "com.google.android.calculator2" -> "com.android.calculator2"
            else -> null
        }
    }



    private fun getAppDisplayName(packageName: String): String? {
        return when (packageName) {
            "com.google.android.deskclock" -> "Clock"
            "com.android.chrome" -> "Chrome"
            "com.google.android.googlequicksearchbox" -> "Google"
            "com.android.settings" -> "Settings"
            "com.google.android.calculator2" -> "Calculator"
            "com.android.camera2" -> "Camera"
            "com.google.android.gm" -> "Gmail"
            else -> {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    fun debugInstalledApps() {
        Log.d(TAG, "🔍 DEBUGGING: Checking installed apps...")

        val testPackages = listOf(
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.google.android.calculator2",
            "com.android.calculator2",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "com.android.settings"
        )

        testPackages.forEach { packageName ->
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                Log.d(TAG, "✅ PACKAGE FOUND: $packageName")
                Log.d(TAG, "   - Version: ${packageInfo.versionName}")
                Log.d(TAG, "   - Launch Intent: ${intent != null}")
                if (intent != null) {
                    Log.d(TAG, "   - Intent Action: ${intent.action}")
                    Log.d(TAG, "   - Intent Component: ${intent.component}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "❌ PACKAGE NOT FOUND: $packageName - ${e.message}")
            }
        }

        // List ALL installed apps with Clock in the name
        try {
            val allApps = packageManager.getInstalledApplications(0)
            val clockApps = allApps.filter {
                val label = packageManager.getApplicationLabel(it).toString().lowercase()
                label.contains("clock") || label.contains("alarm") || it.packageName.contains("clock")
            }

            Log.d(TAG, "🕐 CLOCK-RELATED APPS FOUND:")
            clockApps.forEach { appInfo ->
                val label = packageManager.getApplicationLabel(appInfo)
                Log.d(TAG, "   - $label (${appInfo.packageName})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing apps: ${e.message}")
        }
    }
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        serviceScope.cancel() // AJOUTER cette ligne
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::textToSpeech.isInitialized) {
                textToSpeech.stop()
                textToSpeech.shutdown()
            }
            instance = null
            Log.d(TAG, "Service détruit proprement")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur destruction service: ${e.message}")
        }
    }
}
// Hada dyal Bilal

package com.voiceassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
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

class VoiceAssistantAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech

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
        Log.d(TAG, "Service d'accessibilité connecté - Autoclick disponible")
        speakText("Service d'accessibilité Auralia activé")
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
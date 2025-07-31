package com.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voiceassistant.service.VoiceAssistantService
import com.voiceassistant.ui.screens.WelcomeScreen
import com.voiceassistant.ui.theme.VoiceAssistantTheme
import java.util.*
import com.voiceassistant.ui.screens.MainAssistantScreen
import com.voiceassistant.ui.screens.SettingsScreen
import com.voiceassistant.ui.screens.HistoryScreen
import com.voiceassistant.ui.screens.VoiceCommand
import androidx.compose.ui.text.font.FontWeight
import com.voiceassistant.accessibility.VoiceAssistantAccessibilityService

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private var isServiceRunning by mutableStateOf(false)
    private var currentScreen by mutableStateOf("welcome")
    private var isListening by mutableStateOf(false)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startVoiceAssistantService()
        } else {
            speakText("Permissions are required for the voice assistant to work properly")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DebugTest", "MainActivity onCreate called")

        textToSpeech = TextToSpeech(this, this)

        setContent {
            VoiceAssistantTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                when (currentScreen) {
                    "welcome" -> {
                        WelcomeScreen()
                    }
                    "main" -> {
                        MainAssistantScreen(
                            isListening = isListening,
                            onSetAlarm = { handleSetAlarm() },
                            onSendSMS = { handleSendSMS() },
                            onManageApps = { handleManageApps() },
                            onWebNavigation = { handleWebNavigation() },
                            onMenuClick = { handleMenuClick() },
                            onProfileClick = { handleProfileClick() }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            onBackClick = {
                                currentScreen = "main" // Retour à l'écran principal
                                speakText("Returning to main screen")
                            },
                            onAppLanguageClick = { handleAppLanguageChange() },
                            onVoiceLanguageClick = { handleVoiceLanguageChange() },
                            onThemeClick = { handleThemeChange() },
                            onWakeWordClick = { handleWakeWordChange() },
                            onFontSizeClick = { handleFontSizeChange() }
                        )
                    }
                    "history" -> {
                        HistoryScreen(
                            onBackClick = {
                                currentScreen = "main"
                                speakText("Returning to main screen")
                            },
                            onPlayCommand = { command -> handlePlayCommand(command) },
                            onEditCommand = { command -> handleEditCommand(command) }
                        )
                    }
                    "test" -> {
                        SimpleTestScreen(
                            onBackClick = {
                                currentScreen = "main"
                                speakText("Returning to main screen")
                            }
                        )
                    }
                }
            }
        }


        checkPermissions()
        //jutse pour pour tester la deuxieme page sinon apres elle va etre supprime
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentScreen == "welcome") {
                navigateToMainScreen()
            }
        }, 3000)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startVoiceAssistantService()
        }
    }

    private fun startVoiceAssistantService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        startForegroundService(intent)
        isServiceRunning = true
        speakText("Voice assistant is now active. Say 'Hey Gemma' to start")
    }

    private fun stopVoiceAssistantService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        stopService(intent)
        isServiceRunning = false
        isListening = false
        currentScreen = "welcome"
        speakText("Voice assistant stopped")
    }

    private fun toggleService() {
        if (isServiceRunning) {
            stopVoiceAssistantService()
        } else {
            startVoiceAssistantService()
        }
    }

    private fun speakInstructions() {
        val instructions = """
            Welcome to your voice assistant. Here are the available commands:
            Say 'Hey Gemma' followed by:
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
        if (::textToSpeech.isInitialized && textToSpeech.isSpeaking.not()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
            speakText("Voice assistant initialized successfully")
        }
    }

    // Méthode appelée quand "Hi Auralia" est détecté
    private fun navigateToMainScreen() {
        currentScreen = "main"
        isListening = true
        speakText("Hi, how can I help you?")
    }

    // Gestionnaires pour les raccourcis
    private fun handleSetAlarm() {
        speakText("Setting alarm. What time would you like to set it?")
        Toast.makeText(this, "Set Alarm activated", Toast.LENGTH_SHORT).show()
    }

    private fun handleSendSMS() {
        speakText("Send SMS. Who would you like to send a message to?")
        Toast.makeText(this, "Send SMS activated", Toast.LENGTH_SHORT).show()
    }

    private fun handleManageApps() {
        currentScreen = "test"
        speakText("Opening autoclick test")
        Toast.makeText(this, "Autoclick Test opened", Toast.LENGTH_SHORT).show()
    }

    private fun handleWebNavigation() {
        speakText("Web navigation. What would you like to search for?")
        Toast.makeText(this, "Web Navigation activated", Toast.LENGTH_SHORT).show()
    }

    private fun handleMenuClick() {
        currentScreen = "settings"
        speakText("Opening settings")
    }

    private fun handleProfileClick() {
        currentScreen = "history"
        speakText("Opening command history")
    }

    private fun handleAppLanguageChange() {
        speakText("App language settings")
        Toast.makeText(this, "App Language clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleVoiceLanguageChange() {
        speakText("Voice language settings")
        Toast.makeText(this, "Voice Language clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleThemeChange() {
        speakText("Theme settings")
        Toast.makeText(this, "Theme clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleWakeWordChange() {
        speakText("Wake word settings")
        Toast.makeText(this, "Wake Word clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleFontSizeChange() {
        speakText("Font size settings")
        Toast.makeText(this, "Font Size clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handlePlayCommand(command: VoiceCommand) {
        speakText("Replaying command: ${command.command}")
        Toast.makeText(this, "Playing: ${command.command}", Toast.LENGTH_SHORT).show()
        // Ici vous pouvez ajouter la logique pour rejouer la commande
    }

    private fun handleEditCommand(command: VoiceCommand) {
        speakText("Editing command")
        Toast.makeText(this, "Editing: ${command.command}", Toast.LENGTH_SHORT).show()
        // Ici vous pouvez ajouter la logique pour éditer la commande
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}
// REMPLACEZ VOS FONCTIONS SimpleTestScreen ET executeTestCommand PAR CECI :

@Composable
fun SimpleTestScreen(
    onBackClick: () -> Unit = {}
) {
    var testResult by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBackClick) {
                    Text("← Retour")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "🎯 Test Autoclick Complet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Statut du service
        item {
            val serviceConnected = VoiceAssistantAccessibilityService.instance != null
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceConnected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (serviceConnected)
                            "✅ Service d'accessibilité connecté"
                        else
                            "❌ Service d'accessibilité non connecté",
                        fontWeight = FontWeight.Bold
                    )
                    if (!serviceConnected) {
                        Text(
                            text = "\nPour l'activer :\n1. Paramètres → Accessibilité\n2. Cherchez 'Voice Assistant'\n3. Activez le service",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Test de connexion
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔗 Test de Connexion",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isExecuting = true
                            executeTestCommand("test simple") { result ->
                                testResult = result
                                isExecuting = false
                            }
                        },
                        enabled = !isExecuting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isExecuting) "Test en cours..." else "Tester la connexion")
                    }
                }
            }
        }

        // Tests de coordonnées
        item {
            TestSection(
                title = "📍 Clic par Coordonnées",
                description = "Teste le clic à des positions précises",
                commands = listOf(
                    "Clique à 300,300" to "Centre approximatif de l'écran",
                    "Clique à 100,200" to "Coin supérieur gauche",
                    "Clique à 500,800" to "Partie basse de l'écran"
                ),
                onExecute = { command ->
                    isExecuting = true
                    executeTestCommand(command) { result ->
                        testResult = result
                        isExecuting = false
                    }
                },
                isExecuting = isExecuting
            )
        }

        // Tests de clic par texte
        item {
            TestSection(
                title = "🔤 Clic par Texte",
                description = "Cherche et clique sur des éléments par leur texte",
                commands = listOf(
                    "Clique sur Tester" to "Cherche un bouton 'Tester'",
                    "Clique sur Retour" to "Cherche le bouton 'Retour'",
                    "Click on Test" to "Version anglaise",
                    "Clique sur Button" to "Cherche 'Button'"
                ),
                onExecute = { command ->
                    isExecuting = true
                    executeTestCommand(command) { result ->
                        testResult = result
                        isExecuting = false
                    }
                },
                isExecuting = isExecuting
            )
        }

        // Tests de saisie
        item {
            TestSection(
                title = "⌨️ Saisie de Texte",
                description = "Saisit du texte dans les champs éditables",
                commands = listOf(
                    "Tape 'Hello World'" to "Saisit 'Hello World'",
                    "Type 'Test 123'" to "Saisit 'Test 123'",
                    "Tape 'Auralia'" to "Saisit 'Auralia'"
                ),
                onExecute = { command ->
                    isExecuting = true
                    executeTestCommand(command) { result ->
                        testResult = result
                        isExecuting = false
                    }
                },
                isExecuting = isExecuting
            )
        }

        // Tests de défilement
        item {
            TestSection(
                title = "📜 Défilement",
                description = "Teste le scroll dans différentes directions",
                commands = listOf(
                    "Scroll down" to "Défile vers le bas",
                    "Scroll up" to "Défile vers le haut",
                    "Scroll left" to "Défile vers la gauche",
                    "Scroll right" to "Défile vers la droite"
                ),
                onExecute = { command ->
                    isExecuting = true
                    executeTestCommand(command) { result ->
                        testResult = result
                        isExecuting = false
                    }
                },
                isExecuting = isExecuting
            )
        }

        // Tests système
        item {
            TestSection(
                title = "🏠 Actions Système",
                description = "⚠️ Ces actions peuvent sortir de l'app",
                commands = listOf(
                    "Go back" to "⚠️ Bouton retour (sort de l'app)",
                    "Go home" to "⚠️ Écran d'accueil (sort de l'app)",
                    "Screenshot" to "Capture d'écran",
                    "Open notifications" to "Ouvre le panneau de notifications"
                ),
                onExecute = { command ->
                    isExecuting = true
                    executeTestCommand(command) { result ->
                        testResult = result
                        isExecuting = false
                    }
                },
                isExecuting = isExecuting,
                isWarning = true
            )
        }

        // Résultats
        if (testResult.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            testResult.contains("succès") -> MaterialTheme.colorScheme.primaryContainer
                            testResult.contains("non disponible") -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 Dernier Résultat",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testResult,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Instructions d'utilisation
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Instructions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val instructions = listOf(
                        "• Activez d'abord le service d'accessibilité",
                        "• Testez la connexion avant les autres commandes",
                        "• Les coordonnées sont relatives à la taille de l'écran",
                        "• Le scroll fonctionne mieux dans des apps avec contenu",
                        "• Les actions système peuvent vous faire sortir de l'app",
                        "• Vérifiez les logs avec: adb logcat | grep AutoclickService"
                    )

                    instructions.forEach { instruction ->
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestSection(
    title: String,
    description: String,
    commands: List<Pair<String, String>>,
    onExecute: (String) -> Unit,
    isExecuting: Boolean,
    isWarning: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            commands.forEach { (command, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = command,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onExecute(command) },
                        enabled = !isExecuting,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (isExecuting) "..." else "Test")
                    }
                }

                if (command != commands.last().first) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

fun executeTestCommand(command: String, onResult: (String) -> Unit) {
    val service = VoiceAssistantAccessibilityService.instance

    if (service != null) {
        try {
            Log.d("TestAutoclick", "Exécution commande: $command")
            val success = service.executeGenericCommand(command)
            val result = if (success) {
                "✅ Commande exécutée avec succès"
            } else {
                "❌ Échec de l'exécution de la commande"
            }
            Log.d("TestAutoclick", "Résultat: $result")
            onResult(result)
        } catch (e: Exception) {
            val errorResult = "❌ Erreur: ${e.message}"
            Log.e("TestAutoclick", "Erreur: ${e.message}", e)
            onResult(errorResult)
        }
    } else {
        val noServiceResult = "❌ Service d'accessibilité non disponible.\n\nActivez-le dans :\nParamètres → Accessibilité → Voice Assistant"
        Log.w("TestAutoclick", "Service non disponible")
        onResult(noServiceResult)
    }
}
@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    onSpeakInstructions: () -> Unit
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

        Text(
            text = "Say 'Hey Gemma' to activate voice commands",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics {
                contentDescription = "To use the assistant, say the wake word 'Hey Gemma' followed by your command"
            }
        )
    }
}
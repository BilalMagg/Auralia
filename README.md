# 🎤 Auralia - Assistant Vocal Intelligent

[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/about/versions/android-14.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table des Matières

- [🎯 Vue d'ensemble](#-vue-densemble)
- [✨ Fonctionnalités](#-fonctionnalités)
- [🏗️ Architecture](#️-architecture)
- [🚀 Installation](#-installation)
- [⚙️ Configuration](#️-configuration)
- [📱 Utilisation](#-utilisation)
- [🔧 Développement](#-développement)
- [📚 Documentation](#-documentation)
- [🤝 Contribution](#-contribution)
- [📄 Licence](#-licence)

## 🎯 Vue d'ensemble

**Auralia** est un assistant vocal intelligent pour Android qui combine reconnaissance vocale avancée, traitement du langage naturel et automatisation des tâches quotidiennes. L'application utilise des technologies modernes comme Jetpack Compose, Ollama pour l'IA locale, et Android SpeechRecognizer pour la reconnaissance vocale.

### 🎯 Objectifs du Projet

- **Assistant vocal complet** avec wake word "Hey Gemma"
- **Reconnaissance vocale hors ligne** pour la confidentialité
- **IA locale** avec Gemma 3n via Ollama
- **Automatisation des tâches** quotidiennes
- **Design accessible** en priorité
- **Interface moderne** avec Jetpack Compose

## ✨ Fonctionnalités

### 🎤 Wake Word Activation
- **"Hey Gemma"** - Activation vocale de l'assistant
- **Détection en temps réel** avec Porcupine
- **Activation mains libres** pour une utilisation naturelle
- **Feedback visuel** lors de la détection

### 🗣️ Reconnaissance Vocale Hors Ligne
- **Android SpeechRecognizer** intégré pour la reconnaissance vocale
- **Fonctionnement hors ligne** selon les capacités de l'appareil
- **Support multilingue** natif Android
- **Reconnaissance en temps réel** avec feedback immédiat
- **Suppression de bruit** automatique

### 🤖 IA Locale avec Gemma 3n
- **Modèle Gemma 3n** via Ollama pour le traitement du langage
- **Traitement local** pour la confidentialité
- **Réponses intelligentes** et contextuelles
- **Configuration serveur** Ollama personnalisable
- **Gestion des erreurs** robuste

### 🔊 Synthèse Vocale (Text-to-Speech)
- **Feedback vocal** pour toutes les interactions
- **Synthèse multilingue** intégrée Android
- **Voix naturelle** et configurable
- **Contrôle de la vitesse** et du pitch

### ⚡ Automatisation des Tâches Quotidiennes
- **Gestion des alarmes** et rappels
- **Envoi de messages** SMS
- **Navigation web** et recherche
- **Ouverture d'applications**
- **Gestion des contacts**
- **Commandes système** avancées

### ♿ Design Accessible en Priorité
- **Service d'accessibilité** intégré
- **Navigation vocale** complète
- **Interface adaptée** aux utilisateurs malvoyants
- **Feedback haptique** et sonore
- **Contrôles alternatifs** pour tous les utilisateurs

### 🎨 Interface Compose Moderne
- **Jetpack Compose** pour l'interface utilisateur
- **Configuration intuitive** via l'interface
- **Feedback visuel** en temps réel
- **Thèmes adaptatifs** (clair/sombre)
- **Animations fluides** et réactives

## 🏗️ Architecture

### Structure du Projet
```
app/src/main/java/com/voiceassistant/
├── 📱 MainActivity.kt                    # Point d'entrée principal
├── 🎤 stt/                              # Reconnaissance vocale
│   ├── AudioRecorder.kt                 # Enregistrement audio haute qualité
│   ├── AndroidSpeechRecognizer.kt      # Reconnaissance Android native
│   └── SpeechToTextManager.kt          # Gestionnaire principal
├── 🤖 agent/                           # Agents IA
│   ├── VoiceAgent.kt                   # Agent vocal principal
│   ├── core/                           # Composants de base
│   ├── parser/                         # Analyseurs de commandes
│   └── example/                        # Exemples d'agents
├── 📋 commands/                        # Traitement des commandes
│   └── CommandProcessor.kt             # Processeur de commandes
├── 🌐 network/                         # Communication réseau
│   └── OllamaApiClient.kt              # Client API Ollama
├── 📊 viewmodel/                       # ViewModels
│   ├── SpeechToTextViewModel.kt        # VM reconnaissance vocale
│   └── ImageAnalysisViewModel.kt       # VM analyse d'images
├── 🎨 ui/screens/                      # Écrans de l'interface
│   ├── WelcomeScreen.kt                # Écran d'accueil
│   ├── MainAssistantScreen.kt          # Écran principal
│   ├── SpeechToTextScreen.kt           # Écran reconnaissance vocale
│   ├── ImageAnalysisScreen.kt          # Écran analyse d'images
│   ├── LlamaScreen.kt                  # Écran LLaMA
│   ├── AgentScreen.kt                  # Écran agents
│   ├── SettingsScreen.kt               # Écran paramètres
│   └── ServerConfigScreen.kt           # Configuration serveur
├── 🔧 service/                         # Services Android
│   └── VoiceAssistantService.kt       # Service principal
├── ♿ accessibility/                    # Service d'accessibilité
│   └── VoiceAssistantAccessibilityService.kt
├── 🧠 model/                           # Modèles de données
├── 📚 repository/                      # Couche de données
├── 🛠️ utils/                          # Utilitaires
├── ⚙️ config/                         # Configuration
└── 🔬 tflite/                         # TensorFlow Lite
```

### Technologies Utilisées

| Composant | Technologie | Version |
|-----------|-------------|---------|
| **UI** | Jetpack Compose | 1.5+ |
| **Langage** | Kotlin | 1.9+ |
| **Architecture** | MVVM | - |
| **IA Locale** | Ollama + Gemma 3n | - |
| **Reconnaissance Vocale** | Android SpeechRecognizer | Natif |
| **Wake Word** | Porcupine | 3.0.1 |
| **Réseau** | Retrofit + OkHttp | 2.9.0 |
| **Images** | Coil | 2.5.0 |

## 🚀 Installation

### Prérequis

- **Android Studio** Arctic Fox ou plus récent
- **Android SDK** API 24+ (Android 7.0)
- **Kotlin** 1.9+
- **Gradle** 8.0+
- **Serveur Ollama** avec Gemma 3n (pour les fonctionnalités IA)

### Installation Rapide

1. **Cloner le repository**
   ```bash
   git clone https://github.com/votre-username/Auralia2.git
   cd Auralia2
   ```

2. **Ouvrir dans Android Studio**
   ```bash
   android-studio .
   ```

3. **Synchroniser Gradle**
   - Attendre la synchronisation automatique
   - Ou cliquer sur "Sync Now" si demandé

4. **Installer sur appareil**
   - Connecter un appareil Android ou lancer un émulateur
   - Cliquer sur "Run" (▶️)

### Configuration Ollama avec Gemma 3n

1. **Installer Ollama** sur votre ordinateur
   ```bash
   # macOS/Linux
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Windows
   # Télécharger depuis https://ollama.ai/download
   ```

2. **Installer Gemma 3n**
   ```bash
   ollama pull gemma2:3n
   ```

3. **Démarrer le serveur**
   ```bash
   ollama serve
   ```

## ⚙️ Configuration

### Permissions Requises

L'application demande automatiquement les permissions suivantes :

```xml
<!-- Audio et reconnaissance vocale -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Communication -->
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />

<!-- Images et caméra -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- Services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```

### Configuration du Serveur Ollama

1. **Ouvrir l'application**
2. **Aller dans Paramètres** (menu hamburger)
3. **Section "Configuration Serveur"**
4. **Taper l'URL du serveur** : `http://VOTRE_IP:11434/`
5. **Sauvegarder la configuration**

### Configuration du Wake Word

L'application utilise **"Hey Gemma"** comme wake word :
- **Activation automatique** lors de la détection
- **Feedback visuel** et sonore
- **Configuration** via l'interface Compose

## 📱 Utilisation

### 🎤 Activation et Commandes Vocales

1. **Lancer l'application**
2. **Autoriser les permissions** demandées
3. **Activer le service d'accessibilité** (recommandé)
4. **Dire "Hey Gemma"** pour activer l'assistant
5. **Utiliser les commandes vocales** :
   - "Set alarm for 8 AM"
   - "Send message to John saying hello"
   - "Open Chrome"
   - "What's the weather like?"
   - "Set a reminder for tomorrow"

### 🗣️ Reconnaissance Vocale

1. **Aller dans "Speech to Text"**
2. **Autoriser l'accès au microphone**
3. **Taper "Start Listening"**
4. **Parler clairement** dans le microphone
5. **Voir la transcription** en temps réel
6. **La reconnaissance s'arrête** automatiquement

### 🤖 Interaction avec Gemma 3n

1. **Activer avec "Hey Gemma"**
2. **Poser une question** ou donner une instruction
3. **Recevoir une réponse** intelligente de Gemma 3n
4. **Feedback vocal** automatique

### ⚡ Automatisation des Tâches

#### Alarmes et Rappels
- "Set alarm for 7 AM"
- "Remind me to call mom tomorrow"
- "Set timer for 30 minutes"

#### Messages et Communication
- "Send message to [contact] saying [message]"
- "Call [contact name]"
- "Read my last messages"

#### Navigation et Applications
- "Open [app name]"
- "Search for [query] on Google"
- "Navigate to [location]"

## 🔧 Développement

### Structure de Développement

#### Ajouter une Nouvelle Commande

1. **Modifier** `CommandProcessor.kt`
2. **Ajouter la logique** dans `processCommand()`
3. **Implémenter la fonction** de traitement
4. **Tester** avec l'assistant vocal

```kotlin
// Exemple d'ajout de commande
when {
    lowerCommand.startsWith("nouvelle commande") -> handleNouvelleCommande(lowerCommand)
    // ... autres commandes
}

private fun handleNouvelleCommande(command: String) {
    // Logique de la nouvelle commande
    speakText("Nouvelle commande exécutée")
}
```

#### Configuration du Build

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
```

### Tests

```bash
# Tests unitaires
./gradlew test

# Tests instrumentés
./gradlew connectedAndroidTest

# Build de release
./gradlew assembleRelease
```

### Debugging

#### Logs Utiles

```kotlin
// Dans le code
Log.d("Auralia", "Message de debug")
Log.e("Auralia", "Erreur", exception)

// Filtrer dans Android Studio
// Tag: Auralia
```

#### Problèmes Courants

1. **Wake word ne fonctionne pas**
   - Vérifier les permissions microphone
   - Contrôler que Porcupine est correctement configuré

2. **Erreur de connexion Ollama**
   - Vérifier que le serveur Ollama tourne
   - Contrôler que Gemma 3n est installé
   - Tester avec le bouton "Test Connection"

3. **Reconnaissance vocale ne fonctionne pas**
   - Vérifier les permissions microphone
   - Contrôler que SpeechRecognizer est disponible

## 📚 Documentation

### Guides Détaillés

- **[STREAMING_SETUP.md](STREAMING_SETUP.md)** - Configuration du streaming mot par mot
- **[SPEECH_TO_TEXT_SETUP.md](SPEECH_TO_TEXT_SETUP.md)** - Configuration reconnaissance vocale
- **[IMAGE_ANALYSIS_SETUP.md](IMAGE_ANALYSIS_SETUP.md)** - Configuration analyse d'images
- **[OLLAMA_SERVER_SETUP.md](OLLAMA_SERVER_SETUP.md)** - Configuration serveur Ollama
- **[OLLAMA_SETUP.md](OLLAMA_SETUP.md)** - Installation et configuration Ollama

### API Reference

#### OllamaApiClient
```kotlin
class OllamaApiClient {
    suspend fun generateText(prompt: String): String
    suspend fun analyzeImage(imageBase64: String, prompt: String): Flow<String>
    suspend fun testConnection(): Boolean
}
```

#### SpeechToTextManager
```kotlin
class SpeechToTextManager {
    fun startListening(onComplete: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
    val transcriptionResult: StateFlow<String>
    val isListening: StateFlow<Boolean>
}
```

#### CommandProcessor
```kotlin
class CommandProcessor {
    fun processCommand(command: String)
    fun handleCallCommand(command: String)
    fun handleSmsCommand(command: String)
    fun handleOpenAppCommand(command: String)
}
```

## 🤝 Contribution

### Comment Contribuer

1. **Fork** le repository
2. **Créer** une branche pour votre fonctionnalité
   ```bash
   git checkout -b feature/nouvelle-fonctionnalite
   ```
3. **Développer** votre fonctionnalité
4. **Tester** exhaustivement
5. **Commit** vos changements
   ```bash
   git commit -m "feat: ajouter nouvelle fonctionnalité"
   ```
6. **Push** vers votre fork
   ```bash
   git push origin feature/nouvelle-fonctionnalite
   ```
7. **Créer** une Pull Request

### Standards de Code

- **Kotlin** avec conventions officielles
- **Jetpack Compose** pour l'UI
- **MVVM** pour l'architecture
- **Tests unitaires** pour la logique métier
- **Documentation** en français
- **Accessibilité** en priorité

### Fonctionnalités Suggérées

- [ ] Support multilingue complet
- [ ] Intégration avec d'autres assistants IA
- [ ] Mode voiture optimisé
- [ ] Widgets Android
- [ ] Intégration calendrier avancée
- [ ] Support des notifications
- [ ] Intégration domotique
- [ ] Mode hors ligne complet

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

## 🙏 Remerciements

- **Ollama** pour l'infrastructure IA locale
- **Google Gemma** pour le modèle de langage
- **Android SpeechRecognizer** pour la reconnaissance vocale native
- **Picovoice** pour le wake word detection
- **Jetpack Compose** pour l'interface moderne
- **La communauté Android** pour les outils et bibliothèques

## 📞 Support

- **Issues** : [GitHub Issues](https://github.com/votre-username/Auralia2/issues)
- **Discussions** : [GitHub Discussions](https://github.com/votre-username/Auralia2/discussions)
- **Email** : support@auralia.app

---

**Auralia** - Votre assistant vocal intelligent et privé avec Gemma 🤖✨ 

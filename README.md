# 🎤 Auralia - Intelligent Voice Assistant

[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/about/versions/android-14.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [🎯 Overview](#-overview)
- [✨ Implemented Features](#-implemented-features)
- [🔍 How It Works](#-how-it-works)
- [🏗️ Architecture](#️-architecture)
- [🚀 Installation](#-installation)
- [⚙️ Configuration](#️-configuration)
- [📱 Usage](#-usage)
- [🔧 Development](#-development)
- [📚 Documentation](#-documentation)
- [🤝 Contributing](#-contributing)
- [🔮 Future Features](#-future-features)
- [📄 License](#-license)

## 🎯 Overview

**Auralia** is an intelligent voice assistant for Android that uses a unique **visual-AI approach** to understand and execute commands. When you speak a command, the app takes a screenshot of your current screen, analyzes it with LLaVA (vision model), and then processes the command with Gemma 3n (text model) for intelligent task execution.

### 🎯 Project Goals

- **Visual-AI powered** voice assistant with "Hey Aura" wake word
- **Seamless hands-free experience** (no clicks needed)
- **Screenshot-based context understanding** using LLaVA
- **Intelligent command processing** with Gemma 3n
- **Task automation** (alarms, web search, apps, etc.)
- **Accessibility-first design**
- **Modern interface** with Jetpack Compose

## ✨ Implemented Features

### 🎤 Wake Word Detection
- **"Hey Aura"** wake word using Porcupine
- **Real-time detection** with audio processing
- **Background service** for continuous listening
- **Automatic transition** to command mode (no clicks needed)
- **Seamless hands-free experience**
- **Visual feedback** during detection
- **Fallback mode** if Porcupine fails

### 🗣️ Speech Recognition
- **Android SpeechRecognizer** for voice input
- **Offline capability** depending on device
- **Multi-language support** (system default)
- **Real-time transcription** with feedback
- **Error handling** for recognition failures

### 📸 Visual-AI Command Processing
- **Automatic screenshot capture** when command is given
- **LLaVA analysis** of screen content for context
- **Gemma 3n processing** of command with visual context
- **Intelligent task execution** based on screen understanding
- **Real-time response** with visual feedback

### 🔊 Text-to-Speech
- **Voice feedback** for all interactions
- **Multi-language synthesis** (system default)
- **Natural voice output** for responses
- **Speed and pitch control** available
- **Queue management** for multiple responses

### ⚡ Smart Task Automation
- **Alarm and reminder** setting with time recognition
- **Web search and navigation** based on screen context
- **Application launching** and management
- **Phone calls** to contacts by name
- **SMS messaging** with contact lookup
- **Time queries** with voice response
- **Help system** with command list
- **Contact management** integration

### 🤖 AI Image Analysis
- **LLaVA model** integration via Ollama
- **Image analysis** from camera/gallery
- **Streaming responses** word-by-word
- **Custom prompts** for analysis
- **Error handling** for network issues

### ♿ Accessibility Features
- **Accessibility service** for system integration
- **Notification reading** capability
- **Voice navigation** throughout app
- **Screen reader** compatibility
- **Alternative input methods**

### 🎨 User Interface
- **Jetpack Compose** modern UI
- **Multiple screens**: Welcome, Main, Settings, etc.
- **Real-time status** indicators
- **Dark theme** support
- **Responsive design** for different screen sizes

### 🔧 System Integration
- **Foreground service** for background operation
- **Permission management** for all features
- **Notification system** for service status
- **Settings configuration** for customization
- **Server configuration** for Ollama

## 🔍 How It Works

### 🎯 Visual-AI Command Processing Workflow

```
1. User says "Hey Aura" → Wake word detection
2. Automatically transitions to command mode (no clicks needed)
3. User gives command → Speech recognition
4. App takes screenshot → Screen capture
5. LLaVA analyzes screenshot → Visual context understanding
6. Command + visual context sent to Gemma 3n → Intelligent processing
7. Gemma 3n executes task → Task automation
8. Voice feedback → Text-to-speech response
```

### 📱 Example Use Cases

#### Setting Alarms
- **Command**: "Set alarm at 6 PM"
- **Process**: 
  1. Screenshot shows current time/clock app
  2. LLaVA identifies time context
  3. Gemma 3n processes "set alarm at 6 PM" with visual context
  4. App sets alarm for 6:00 PM

#### Web Search
- **Command**: "Search for Android development"
- **Process**:
  1. Screenshot shows current browser/app
  2. LLaVA identifies search context
  3. Gemma 3n processes search command with visual context
  4. App opens browser and searches "Android development"

#### App Management
- **Command**: "Open settings"
- **Process**:
  1. Screenshot shows current app/screen
  2. LLaVA identifies navigation context
  3. Gemma 3n processes "open settings" with visual context
  4. App launches Android Settings

#### Smart Responses
- **Command**: "What's on my screen?"
- **Process**:
  1. Screenshot captured
  2. LLaVA analyzes screen content
  3. Gemma 3n generates description
  4. Voice response describes what's visible

### 🔄 Real-time Processing

- **Instant screenshot capture** when command is detected
- **Automatic wake word to command transition** (no user interaction)
- **Parallel processing** of LLaVA and Gemma 3n
- **Streaming responses** for immediate feedback
- **Error recovery** if AI models fail
- **Fallback to basic commands** if needed

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/voiceassistant/
├── 📱 MainActivity.kt                    # Main entry point
├── 🎤 stt/                              # Speech recognition
│   ├── AudioRecorder.kt                 # High-quality audio recording
│   ├── AndroidSpeechRecognizer.kt      # Native Android recognition
│   └── SpeechToTextManager.kt          # Main manager
├── 🤖 agent/                           # AI Agents
│   ├── VoiceAgent.kt                   # Main voice agent
│   ├── core/                           # Core components
│   ├── parser/                         # Command parsers
│   └── example/                        # Agent examples
├── 📋 commands/                        # Command processing
│   └── CommandProcessor.kt             # Command processor
├── 🌐 network/                         # Network communication
│   ├── OllamaApiClient.kt              # Ollama API client
│   ├── OllamaApiService.kt             # API service interface
│   └── LlamaApi.kt                     # LLaMA API interface
├── 📊 viewmodel/                       # ViewModels
│   ├── SpeechToTextViewModel.kt        # Speech recognition VM
│   └── ImageAnalysisViewModel.kt       # Image analysis VM
├── 🎨 ui/screens/                      # UI screens
│   ├── WelcomeScreen.kt                # Welcome screen
│   ├── MainAssistantScreen.kt          # Main screen
│   ├── SpeechToTextScreen.kt           # Speech recognition screen
│   ├── ImageAnalysisScreen.kt          # Image analysis screen
│   ├── LlamaScreen.kt                  # LLaMA screen
│   ├── AgentScreen.kt                  # Agents screen
│   ├── SettingsScreen.kt               # Settings screen
│   └── ServerConfigScreen.kt           # Server configuration
├── 🔧 service/                         # Android services
│   └── VoiceAssistantService.kt       # Main service
├── ♿ accessibility/                    # Accessibility service
│   └── VoiceAssistantAccessibilityService.kt
├── 🧠 model/                           # Data models
├── 📚 repository/                      # Data layer
├── 🛠️ utils/                          # Utilities
├── ⚙️ config/                         # Configuration
└── 🔬 tflite/                         # TensorFlow Lite
```

### Technologies Used

| Component | Technology | Version |
|-----------|-------------|---------|
| **UI** | Jetpack Compose | 1.5+ |
| **Language** | Kotlin | 1.9+ |
| **Architecture** | MVVM | - |
| **Visual AI** | LLaVA (Ollama) | - |
| **Text AI** | Gemma 3n (Ollama) | - |
| **Speech Recognition** | Android SpeechRecognizer | Native |
| **Wake Word** | Porcupine | 3.0.1 |
| **Network** | Retrofit + OkHttp | 2.9.0 |
| **Images** | Coil | 2.5.0 |

## 🚀 Installation

### Prerequisites

- **Android Studio** Arctic Fox or newer
- **Android SDK** API 24+ (Android 7.0)
- **Kotlin** 1.9+
- **Gradle** 8.0+
- **Ollama server** with LLaVA and Gemma 3n

### Quick Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Auralia2.git
   cd Auralia2
   ```

2. **Open in Android Studio**
   ```bash
   android-studio .
   ```

3. **Sync Gradle**
   - Wait for automatic synchronization
   - Or click "Sync Now" if prompted

4. **Install on device**
   - Connect an Android device or launch an emulator
   - Click "Run" (▶️)

### Ollama Configuration

1. **Install Ollama** on your computer
   ```bash
   # macOS/Linux
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Windows
   # Download from https://ollama.ai/download
   ```

2. **Install required models**
   ```bash
   # Install LLaVA for visual analysis
   ollama pull llava
   
   # Install Gemma 3n for text processing
   ollama pull gemma2:3n
   ```

3. **Start the server**
   ```bash
   ollama serve
   ```

## ⚙️ Configuration

### Required Permissions

The application automatically requests the following permissions:

```xml
<!-- Audio and speech recognition -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Communication -->
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />

<!-- Images and camera -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- Services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```

### Ollama Server Configuration

1. **Open the application**
2. **Go to Settings** (hamburger menu)
3. **"Server Configuration" section**
4. **Enter server URL** : `http://YOUR_IP:11434/`
5. **Save configuration**

### Wake Word Configuration

The application uses **"Hey Gemma"** as wake word:
- **Automatic activation** upon detection
- **Visual and audio feedback**
- **Configuration** through Compose interface

## 📱 Usage

### 🎤 Voice Activation and Commands

1. **Launch the application**
2. **Grant requested permissions**
3. **Enable accessibility service** (recommended)
4. **Say "Hey Aura"** to activate the assistant
5. **Automatically transitions to command mode** (no clicks needed)
6. **Give commands** with visual context:
   - "Set alarm at 6 PM" (while on clock app)
   - "Search for Android development" (while on browser)
   - "Open settings" (from any screen)
   - "What's on my screen?" (describes current view)
   - "Call [contact name]"
   - "Send message to [contact] saying [message]"

### 🗣️ Speech Recognition

1. **Go to "Speech to Text"**
2. **Grant microphone access**
3. **Tap "Start Listening"**
4. **Speak clearly** into the microphone
5. **See transcription** in real-time
6. **Recognition stops** automatically

### 🤖 AI Image Analysis

1. **Go to "Image Analysis" screen**
2. **Select an image** from camera or gallery
3. **Analyze with LLaVA** via Ollama
4. **View streaming results** word by word

### ⚡ Smart Task Automation

#### Time and Alarms
- "Set alarm at 6 PM" (with clock app context)
- "Set reminder for tomorrow at 9 AM"
- "What time is it?"

#### Web and Search
- "Search for Android development" (with browser context)
- "Open Google Maps"
- "Navigate to [location]"

#### Applications and System
- "Open settings" (from any screen)
- "Open [app name]"
- "What's on my screen?"

#### Communication
- "Call [contact name]"
- "Send message to [contact] saying [message]"

#### Smart Assistance
- "Help" (context-aware help)
- "Read notifications" (requires accessibility setup)

## 🔧 Development

### Development Structure

#### Adding a New Command

1. **Modify** `CommandProcessor.kt`
2. **Add logic** in `processCommand()`
3. **Implement the function** for processing
4. **Test** with the voice assistant

```kotlin
// Example of adding a command
when {
    lowerCommand.startsWith("new command") -> handleNewCommand(lowerCommand)
    // ... other commands
}

private fun handleNewCommand(command: String) {
    // Logic for the new command
    speakText("New command executed")
}
```

#### Visual-AI Integration

```kotlin
// Example of visual-AI command processing
private fun processVisualCommand(command: String, screenshot: Bitmap) {
    // 1. Convert screenshot to base64
    val imageBase64 = convertBitmapToBase64(screenshot)
    
    // 2. Analyze with LLaVA
    val visualContext = llavaClient.analyzeImage(imageBase64, "Describe what's on screen")
    
    // 3. Process with Gemma 3n
    val fullPrompt = "Command: $command\nVisual Context: $visualContext\nExecute this command."
    val response = gemmaClient.processCommand(fullPrompt)
    
    // 4. Execute response
    executeCommand(response)
}
```

#### Build Configuration

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

### Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Release build
./gradlew assembleRelease
```

### Debugging

#### Useful Logs

```kotlin
// In the code
Log.d("Auralia", "Debug message")
Log.e("Auralia", "Error", exception)

// Filter in Android Studio
// Tag: Auralia
```

#### Common Issues

1. **Wake word not working**
   - Check microphone permissions
   - Verify Porcupine is properly configured

2. **Ollama connection error**
   - Verify Ollama server is running
   - Check that LLaVA and Gemma 3n are installed
   - Test with "Test Connection" button

3. **Visual-AI not working**
   - Check screenshot permissions
   - Verify both LLaVA and Gemma 3n models are loaded
   - Test individual model connections

## 📚 Documentation

### Detailed Guides

- **[STREAMING_SETUP.md](STREAMING_SETUP.md)** - Word-by-word streaming configuration
- **[SPEECH_TO_TEXT_SETUP.md](SPEECH_TO_TEXT_SETUP.md)** - Speech recognition configuration
- **[IMAGE_ANALYSIS_SETUP.md](IMAGE_ANALYSIS_SETUP.md)** - Image analysis configuration
- **[OLLAMA_SERVER_SETUP.md](OLLAMA_SERVER_SETUP.md)** - Ollama server configuration
- **[OLLAMA_SETUP.md](OLLAMA_SETUP.md)** - Ollama installation and configuration

### API Reference

#### OllamaApiClient
```kotlin
class OllamaApiClient {
    suspend fun analyzeImage(imageBase64: String, prompt: String): Result<OllamaResponse>
    fun analyzeImageStream(imageBase64: String, prompt: String): Flow<String>
    suspend fun processCommand(command: String): Result<String>
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
    fun processCommand(command: String, screenshot: Bitmap?)
    fun handleCallCommand(command: String)
    fun handleSmsCommand(command: String)
    fun handleOpenAppCommand(command: String)
    fun handleAlarmCommand(command: String)
    fun handleSearchCommand(command: String)
}
```

## 🤝 Contributing

### How to Contribute

1. **Fork** the repository
2. **Create** a branch for your feature
   ```bash
   git checkout -b feature/new-feature
   ```
3. **Develop** your feature
4. **Test** thoroughly
5. **Commit** your changes
   ```bash
   git commit -m "feat: add new feature"
   ```
6. **Push** to your fork
   ```bash
   git push origin feature/new-feature
   ```
7. **Create** a Pull Request

### Code Standards

- **Kotlin** with official conventions
- **Jetpack Compose** for UI
- **MVVM** for architecture
- **Unit tests** for business logic
- **Documentation** in English
- **Accessibility** as priority

## 🔮 Future Features

### 🚀 High Priority Features

#### Enhanced Visual-AI
- [ ] **Multi-screen analysis** for complex tasks
- [ ] **Gesture recognition** from screenshots
- [ ] **OCR integration** for text extraction
- [ ] **Object detection** for better context
- [ ] **Screen state tracking** over time

#### Advanced AI Integration
- [ ] **Conversation memory** with visual context
- [ ] **Predictive assistance** based on screen patterns
- [ ] **Custom model fine-tuning** for specific tasks
- [ ] **Offline AI models** for privacy
- [ ] **Multi-model orchestration** for complex tasks

#### Smart Automation
- [ ] **Calendar integration** with visual scheduling
- [ ] **Smart home control** via app interfaces
- [ ] **Email management** with visual composition
- [ ] **Navigation and maps** with visual context
- [ ] **Weather and news** with location awareness

### 🎯 Medium Priority Features

#### User Experience
- [ ] **Personalization** based on usage patterns
- [ ] **Learning algorithms** for user preferences
- [ ] **Gesture control** and motion detection
- [ ] **Augmented reality** interface overlay
- [ ] **Multi-device sync** across platforms

#### Advanced Accessibility
- [ ] **Braille display** support
- [ ] **Eye tracking** for navigation
- [ ] **Brain-computer interface** (BCI) support
- [ ] **Sign language** recognition
- [ ] **Haptic feedback** patterns

#### Security & Privacy
- [ ] **End-to-end encryption** for all communications
- [ ] **Local data processing** only
- [ ] **Privacy controls** and data management
- [ ] **Secure voice authentication**
- [ ] **GDPR compliance** features

### 🌟 Long-term Vision

#### AI Evolution
- [ ] **AGI integration** when available
- [ ] **Predictive assistance** based on patterns
- [ ] **Creative AI** for content generation
- [ ] **Emotional intelligence** capabilities
- [ ] **Autonomous decision making**

#### Platform Expansion
- [ ] **iOS version** development
- [ ] **Web application** interface
- [ ] **Desktop application** (Windows, macOS, Linux)
- [ ] **Smartwatch integration**
- [ ] **Smart glasses** support

#### Enterprise Features
- [ ] **Team collaboration** tools
- [ ] **Business intelligence** integration
- [ ] **Workflow automation** for enterprises
- [ ] **API for third-party** integrations
- [ ] **White-label solutions**

#### Research & Innovation
- [ ] **Open-source AI models** contribution
- [ ] **Research partnerships** with universities
- [ ] **Patent development** for novel features
- [ ] **Academic publications** on voice AI
- [ ] **Community-driven** feature development

### 📋 Implementation Roadmap

#### Phase 1 (Next 3 months)
- [ ] Enhanced visual context understanding
- [ ] Improved Gemma 3n integration
- [ ] Better error handling for AI models
- [ ] Performance optimizations

#### Phase 2 (3-6 months)
- [ ] Multi-screen analysis capabilities
- [ ] Advanced task automation
- [ ] Security enhancements
- [ ] User experience improvements

#### Phase 3 (6-12 months)
- [ ] AI model improvements
- [ ] Platform expansion
- [ ] Enterprise features
- [ ] Advanced accessibility

#### Phase 4 (12+ months)
- [ ] Research integration
- [ ] Innovation features
- [ ] Global expansion
- [ ] Industry partnerships

---

## 📄 License

This project is licensed under MIT. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Ollama** for AI infrastructure
- **Google Gemma** for text processing capabilities
- **LLaVA** for visual understanding
- **Android SpeechRecognizer** for native speech recognition
- **Picovoice** for wake word detection
- **Jetpack Compose** for modern interface
- **The Android community** for tools and libraries

## 📞 Support

- **Issues** : [GitHub Issues](https://github.com/your-username/Auralia2/issues)
- **Discussions** : [GitHub Discussions](https://github.com/your-username/Auralia2/discussions)
- **Email** : support@auralia.app

---

**Auralia** - Your intelligent and private voice assistant 🤖✨ 

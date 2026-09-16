package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MayaVoiceManager
import com.example.audio.SpeechListenState
import com.example.audio.VoicePlaybackState
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MessageRole
import com.example.data.local.UserSettingsEntity
import com.example.data.remote.MayaIntelligenceEngine
import com.example.data.repository.ChatRepository
import com.example.ui.avatar.MayaAvatarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MayaUiState(
    val activeScreen: AppScreen = AppScreen.HOME,
    val avatarState: MayaAvatarState = MayaAvatarState.IDLE,
    val liveTranscript: String = "",
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val showSettings: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val currentlySpeakingMessageId: Long? = null,
    val statusBannerMessage: String = "Hi! I'm MAYA, ready to help you."
)

enum class AppScreen {
    HOME,
    CHAT
}

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao())
    private val intelligenceEngine = MayaIntelligenceEngine()
    val voiceManager = MayaVoiceManager(application)

    private val _uiState = MutableStateFlow(MayaUiState())
    val uiState: StateFlow<MayaUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<UserSettingsEntity?> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettingsEntity()
        )

    val playbackState: StateFlow<VoicePlaybackState> = voiceManager.playbackState
    val listenState: StateFlow<SpeechListenState> = voiceManager.listenState
    val audioVolume: StateFlow<Float> = voiceManager.audioVolume

    init {
        viewModelScope.launch {
            repository.ensureWelcomeMessage()
        }

        // Voice callbacks
        voiceManager.onSpeechRecognized = { recognizedText ->
            _uiState.value = _uiState.value.copy(
                liveTranscript = "",
                isListening = false
            )
            onUserVoiceInput(recognizedText)
        }

        voiceManager.onPartialSpeechRecognized = { partial ->
            _uiState.value = _uiState.value.copy(
                liveTranscript = partial
            )
        }

        // Observe voice manager speaking state to synchronize Avatar
        viewModelScope.launch {
            voiceManager.isSpeakingNow.collect { speaking ->
                _uiState.value = _uiState.value.copy(
                    isSpeaking = speaking,
                    avatarState = if (speaking) {
                        MayaAvatarState.SPEAKING
                    } else if (_uiState.value.isThinking) {
                        MayaAvatarState.THINKING
                    } else if (_uiState.value.isListening) {
                        MayaAvatarState.LISTENING
                    } else {
                        MayaAvatarState.IDLE
                    }
                )
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(activeScreen = screen)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun openClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = true)
    }

    fun closeClearConfirmation() {
        _uiState.value = _uiState.value.copy(showClearConfirmation = false)
    }

    fun startListening() {
        voiceManager.stopSpeaking()
        _uiState.value = _uiState.value.copy(
            isListening = true,
            avatarState = MayaAvatarState.LISTENING,
            liveTranscript = "Listening... Speak now 🎙️",
            statusBannerMessage = "I'm listening to you..."
        )
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            liveTranscript = "",
            avatarState = MayaAvatarState.IDLE,
            statusBannerMessage = "Ready for your question"
        )
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun onUserVoiceInput(text: String) {
        sendMessage(text, isVoiceTriggered = true)
    }

    fun sendMessage(text: String, isVoiceTriggered: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // 1. Insert User Message
            repository.addMessage(
                role = MessageRole.USER,
                content = trimmed
            )

            // 2. Set UI to Thinking State
            _uiState.value = _uiState.value.copy(
                isThinking = true,
                avatarState = MayaAvatarState.THINKING,
                statusBannerMessage = "MAYA is thinking..."
            )

            // 3. Generate response using Gemini or Local Fallback
            val history = repository.getRecentMessages(8)
            val effectiveKey = getEffectiveApiKey()
            val response = intelligenceEngine.getResponse(trimmed, history, effectiveKey)

            // 4. Save MAYA's response
            val mayaMsgId = repository.addMessage(
                role = MessageRole.MAYA,
                content = response
            )

            _uiState.value = _uiState.value.copy(
                isThinking = false,
                statusBannerMessage = "MAYA replied"
            )

            // 5. Speak response if voice triggered or auto-voice is enabled
            val currentSettings = repository.getSettings()
            val shouldSpeak = isVoiceTriggered || currentSettings.autoVoice
            if (shouldSpeak) {
                _uiState.value = _uiState.value.copy(
                    currentlySpeakingMessageId = mayaMsgId,
                    avatarState = MayaAvatarState.SPEAKING
                )
                voiceManager.speak(
                    text = response,
                    pitch = currentSettings.speechPitch,
                    rate = currentSettings.speechRate
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    avatarState = MayaAvatarState.IDLE
                )
            }
        }
    }

    fun speakMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            _uiState.value = _uiState.value.copy(
                currentlySpeakingMessageId = message.id,
                avatarState = MayaAvatarState.SPEAKING,
                statusBannerMessage = "Speaking..."
            )
            voiceManager.speak(
                text = message.content,
                pitch = currentSettings.speechPitch,
                rate = currentSettings.speechRate
            )
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        _uiState.value = _uiState.value.copy(
            currentlySpeakingMessageId = null,
            avatarState = MayaAvatarState.IDLE,
            statusBannerMessage = "Ready"
        )
    }

    fun pauseSpeaking() {
        voiceManager.pauseSpeaking()
    }

    fun replayVoice() {
        voiceManager.replayLast()
    }

    fun regenerateLast() {
        viewModelScope.launch {
            val all = repository.getRecentMessages(4)
            val lastUserMsg = all.firstOrNull { it.role == MessageRole.USER }
            if (lastUserMsg != null) {
                sendMessage(lastUserMsg.content, isVoiceTriggered = false)
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            voiceManager.stopSpeaking()
            repository.clearHistory()
            _uiState.value = _uiState.value.copy(
                currentlySpeakingMessageId = null,
                avatarState = MayaAvatarState.IDLE,
                statusBannerMessage = "Chat cleared. Ready!"
            )
        }
    }

    fun onAvatarClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                avatarState = MayaAvatarState.HAPPY,
                statusBannerMessage = "MAYA: Hehe, nice to see you! 💜"
            )
            val currentSettings = repository.getSettings()
            val friendlyPhrases = listOf(
                "I'm here! What can I help you with today? 💜",
                "Hi there! Ready for some maths, coding or chat? ✨",
                "Hey! You can tap the microphone to talk to me! 🌸"
            )
            val phrase = friendlyPhrases.random()
            voiceManager.speak(phrase, currentSettings.speechPitch, currentSettings.speechRate)
            delay(2800)
            if (!_uiState.value.isSpeaking) {
                _uiState.value = _uiState.value.copy(avatarState = MayaAvatarState.IDLE)
            }
        }
    }

    fun updateSettings(
        pitch: Float,
        rate: Float,
        autoVoice: Boolean,
        language: String
    ) {
        viewModelScope.launch {
            val current = repository.getSettings()
            val updated = current.copy(
                speechPitch = pitch,
                speechRate = rate,
                autoVoice = autoVoice,
                preferredLanguage = language
            )
            repository.updateSettings(updated)
            voiceManager.configureFemaleVoice(pitch, rate)
        }
    }

    fun getEffectiveApiKey(): String {
        val prefs = getApplication<Application>().getSharedPreferences("maya_prefs", android.content.Context.MODE_PRIVATE)
        val prefKey = prefs.getString("gemini_api_key", "") ?: ""
        if (prefKey.isNotBlank()) return prefKey.trim()
        val dbKey = settings.value?.customApiKey ?: ""
        if (dbKey.isNotBlank()) return dbKey.trim()
        return try {
            com.example.BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() &&
                !key.equals("MY_GEMINI_API_KEY", ignoreCase = true) &&
                !key.equals("TODO", ignoreCase = true)
    }

    fun saveApiKey(apiKey: String) {
        val cleanKey = apiKey.trim()
        val prefs = getApplication<Application>().getSharedPreferences("maya_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", cleanKey).apply()

        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(customApiKey = cleanKey))
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = if (cleanKey.isNotBlank()) "Gemini API Key Saved! 🚀" else "API Key removed (Offline mode)"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

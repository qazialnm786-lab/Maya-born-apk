package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoicePlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    STOPPED
}

enum class SpeechListenState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class MayaVoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private val _playbackState = MutableStateFlow(VoicePlaybackState.IDLE)
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private val _listenState = MutableStateFlow(SpeechListenState.IDLE)
    val listenState: StateFlow<SpeechListenState> = _listenState.asStateFlow()

    private val _audioVolume = MutableStateFlow(0f) // 0f..1f for visualizer
    val audioVolume: StateFlow<Float> = _audioVolume.asStateFlow()

    private val _isSpeakingNow = MutableStateFlow(false)
    val isSpeakingNow: StateFlow<Boolean> = _isSpeakingNow.asStateFlow()

    private var lastSpokenText: String = ""
    private var speechPitch: Float = 1.25f
    private var speechRate: Float = 1.0f

    // Callback when user speech is recognized
    var onSpeechRecognized: ((String) -> Unit)? = null
    var onPartialSpeechRecognized: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                configureFemaleVoice()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playbackState.value = VoicePlaybackState.PLAYING
                        _isSpeakingNow.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _playbackState.value = VoicePlaybackState.IDLE
                        _isSpeakingNow.value = false
                        _audioVolume.value = 0f
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = VoicePlaybackState.IDLE
                        _isSpeakingNow.value = false
                        _audioVolume.value = 0f
                    }
                })
            } else {
                Log.e("MayaVoiceManager", "Failed to initialize TTS")
            }
        }
    }

    fun configureFemaleVoice(pitch: Float = speechPitch, rate: Float = speechRate) {
        this.speechPitch = pitch
        this.speechRate = rate
        val engine = tts ?: return
        try {
            engine.setPitch(pitch)
            engine.setSpeechRate(rate)

            // Try to find a high quality female voice in available engine voices
            val availableVoices = engine.voices
            if (availableVoices != null) {
                val femaleVoice = availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("female") || name.contains("woman") || name.contains("en_us_f") || name.contains("hi_in")) &&
                            !voice.isNetworkConnectionRequired
                } ?: availableVoices.firstOrNull { it.name.lowercase().contains("female") }

                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                } else {
                    engine.language = Locale.ENGLISH
                }
            } else {
                engine.language = Locale.ENGLISH
            }
        } catch (e: Exception) {
            Log.w("MayaVoiceManager", "Error setting voice parameters", e)
        }
    }

    fun speak(text: String, pitch: Float = speechPitch, rate: Float = speechRate) {
        if (!isTtsInitialized || tts == null) {
            initTts()
        }

        // Clean text from markdown formatting (e.g. asterisks, backticks, hashes) for spoken audio
        val cleanText = text
            .replace(Regex("""[*#_`~>\[\]]"""), " ")
            .replace(Regex("""\$\$.*?\$\$"""), "math formula")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanText.isBlank()) return

        lastSpokenText = cleanText
        configureFemaleVoice(pitch, rate)

        // Check if text is mostly Hindi/Devanagari
        val isHindi = cleanText.any { it in '\u0900'..'\u097F' }
        try {
            if (isHindi) {
                val hiLocale = Locale.forLanguageTag("hi-IN")
                if (tts?.isLanguageAvailable(hiLocale) ?: -1 >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = hiLocale
                }
            } else {
                tts?.language = Locale.US
            }
        } catch (e: Exception) {
            // keep fallback
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "maya_speech_${System.currentTimeMillis()}")
        }

        _playbackState.value = VoicePlaybackState.PLAYING
        _isSpeakingNow.value = true
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "maya_speech_utt")
    }

    fun stopSpeaking() {
        tts?.stop()
        _playbackState.value = VoicePlaybackState.STOPPED
        _isSpeakingNow.value = false
        _audioVolume.value = 0f
    }

    fun pauseSpeaking() {
        tts?.stop()
        _playbackState.value = VoicePlaybackState.PAUSED
        _isSpeakingNow.value = false
    }

    fun replayLast() {
        if (lastSpokenText.isNotBlank()) {
            speak(lastSpokenText, speechPitch, speechRate)
        }
    }

    // Speech Recognition
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _listenState.value = SpeechListenState.ERROR
            return
        }

        stopSpeaking() // ensure Maya stops speaking before listening

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _listenState.value = SpeechListenState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _listenState.value = SpeechListenState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize dB into 0..1 range (usually -2 to 10 dB)
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioVolume.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _listenState.value = SpeechListenState.PROCESSING
                        _audioVolume.value = 0f
                    }

                    override fun onError(error: Int) {
                        _listenState.value = SpeechListenState.IDLE
                        _audioVolume.value = 0f
                    }

                    override fun onResults(results: Bundle?) {
                        _listenState.value = SpeechListenState.IDLE
                        _audioVolume.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()
                        if (!spokenText.isNullOrBlank()) {
                            onSpeechRecognized?.invoke(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            onPartialSpeechRecognized?.invoke(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening to you... Speak to MAYA")
        }

        try {
            speechRecognizer?.startListening(intent)
            _listenState.value = SpeechListenState.LISTENING
        } catch (e: Exception) {
            Log.e("MayaVoiceManager", "Error starting speech recognizer", e)
            _listenState.value = SpeechListenState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _listenState.value = SpeechListenState.IDLE
        _audioVolume.value = 0f
    }

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }
}

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.VoicePlaybackState
import com.example.ui.avatar.MayaAnimeAvatar
import com.example.ui.avatar.MayaAvatarState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MayaViewModel

@Composable
fun HomeScreen(
    viewModel: MayaViewModel,
    uiState: com.example.ui.viewmodel.MayaUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Permission launcher for microphone
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val requestMicOrToggle = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleListening()
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Ripple pulse for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF110D26),
                        Color(0xFF090618)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MAYA",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI",
                            color = NeonCyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = "Personal Digital Assistant",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(state = uiState.avatarState)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33A077FF), CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            val hasApiKey = viewModel.hasValidApiKey()
            if (!hasApiKey) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFB703))
                        .border(1.dp, Color(0x66FFB703), RoundedCornerShape(10.dp))
                        .clickable { viewModel.openSettings() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to enter Gemini API Key (API Key daalein)",
                            color = Color(0xFFFFD166),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Enter →",
                        color = Color(0xFFFFB703),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 3D Anime MAYA Avatar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MayaAnimeAvatar(
                    state = uiState.avatarState,
                    audioVolume = audioVolume,
                    onAvatarClick = { viewModel.onAvatarClicked() },
                    modifier = Modifier.testTag("maya_avatar")
                )
            }

            // 3. Status & Transcript Bubble Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isListening && uiState.liveTranscript.isNotBlank()) {
                        Text(
                            text = "🎙️ ${uiState.liveTranscript}",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = uiState.statusBannerMessage,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Active Voice Waveform when speaking or listening
                    AnimatedVisibility(
                        visible = uiState.isSpeaking || uiState.isListening,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AudioWaveformVisualizer(
                            isActive = true,
                            audioVolume = audioVolume,
                            primaryColor = if (uiState.isListening) EmeraldGlow else NeonMagenta,
                            secondaryColor = NeonCyan
                        )
                    }
                }
            }

            // 4. Voice Controls Bar (Play / Pause / Stop)
            AnimatedVisibility(
                visible = uiState.isSpeaking || playbackState == VoicePlaybackState.PLAYING || playbackState == VoicePlaybackState.PAUSED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xDD1B1238)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Voice Active",
                                tint = NeonMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (playbackState == VoicePlaybackState.PAUSED) "Voice Paused" else "MAYA Voice Active",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (playbackState == VoicePlaybackState.PAUSED) {
                                IconButton(
                                    onClick = { viewModel.replayVoice() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume Voice",
                                        tint = NeonCyan
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.pauseSpeaking() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause Voice",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier.size(36.dp),
                                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                    contentColor = NeonMagenta
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Voice",
                                    tint = NeonMagenta
                                )
                            }
                        }
                    }
                }
            }

            // 5. Quick Suggestion Pills Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    "Study tips for exams 📚",
                    "Solve: 3x + 15 = 45 📐",
                    "Hindi me baat karo 🌸",
                    "Write a Python function 💻",
                    "Tell me a fun joke 😄",
                    "Translate 'Hello, how are you?' to Hindi 🌐"
                )

                suggestions.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33231A47))
                            .border(1.dp, Color(0x447C4DFF), RoundedCornerShape(20.dp))
                            .clickable { viewModel.sendMessage(prompt, isVoiceTriggered = false) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 6. Bottom Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Chat View Button
                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.CHAT) },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33231A47))
                        .border(1.dp, Color(0x669D4EDD), CircleShape)
                        .testTag("chat_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Open Chat",
                        tint = NeonViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Main Microphone Button with Pulsing Listening State
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isListening) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .scale(micPulseScale)
                                .clip(CircleShape)
                                .background(EmeraldGlow.copy(alpha = 0.25f))
                        )
                    }

                    FloatingActionButton(
                        onClick = requestMicOrToggle,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("microphone_button"),
                        shape = CircleShape,
                        containerColor = if (uiState.isListening) EmeraldGlow else NeonViolet,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (uiState.isListening) "Stop Listening" else "Start Voice Input",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Quick Voice Mute / Speak Toggle Button
                val isAutoVoice = settings?.autoVoice ?: true
                IconButton(
                    onClick = {
                        val currentSettings = settings
                        if (currentSettings != null) {
                            viewModel.updateSettings(
                                pitch = currentSettings.speechPitch,
                                rate = currentSettings.speechRate,
                                autoVoice = !isAutoVoice,
                                language = currentSettings.preferredLanguage
                            )
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33231A47))
                        .border(1.dp, if (isAutoVoice) Color(0x6600F0FF) else Color(0x33FFFFFF), CircleShape)
                        .testTag("voice_button")
                ) {
                    Icon(
                        imageVector = if (isAutoVoice) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeMute,
                        contentDescription = if (isAutoVoice) "Auto Voice Enabled" else "Auto Voice Disabled",
                        tint = if (isAutoVoice) NeonCyan else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

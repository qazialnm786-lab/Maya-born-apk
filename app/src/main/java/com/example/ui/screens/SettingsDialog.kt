package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.local.UserSettingsEntity
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.MayaViewModel

@Composable
fun SettingsDialog(
    viewModel: MayaViewModel,
    settings: UserSettingsEntity?,
    onDismiss: () -> Unit
) {
    var pitch by remember(settings?.speechPitch) { mutableFloatStateOf(settings?.speechPitch ?: 1.25f) }
    var rate by remember(settings?.speechRate) { mutableFloatStateOf(settings?.speechRate ?: 1.0f) }
    var autoVoice by remember(settings?.autoVoice) { mutableStateOf(settings?.autoVoice ?: true) }
    var language by remember(settings?.preferredLanguage) { mutableStateOf(settings?.preferredLanguage ?: "auto") }

    val hasApiKey = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                !BuildConfig.GEMINI_API_KEY.equals("MY_GEMINI_API_KEY", ignoreCase = true)
    } catch (e: Exception) {
        false
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAYA AI Settings",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Voice & Female Tone
                Text(
                    text = "FEMALE VOICE CUSTOMIZATION",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Pitch
                Text(
                    text = "Voice Pitch: ${"%.2f".format(pitch)}x (Sweet Anime Female)",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        viewModel.voiceManager.configureFemaleVoice(pitch, rate)
                    },
                    valueRange = 0.8f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonMagenta,
                        activeTrackColor = NeonViolet,
                        inactiveTrackColor = Color(0x33A077FF)
                    )
                )

                // Rate / Speed
                Text(
                    text = "Speech Rate: ${"%.2f".format(rate)}x",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Slider(
                    value = rate,
                    onValueChange = {
                        rate = it
                        viewModel.voiceManager.configureFemaleVoice(pitch, rate)
                    },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color(0x3300F0FF)
                    )
                )

                // Test Voice Button
                Button(
                    onClick = {
                        val testPhrase = when (language) {
                            "hindi" -> "नमस्ते! यह मेरी आवाज़ का परीक्षण है। क्या मैं अच्छी लग रही हूँ?"
                            "hinglish" -> "Hi! Yeh meri sweet female voice ka test hai, kaisa lag raha hai?"
                            else -> "Hi! This is MAYA testing my voice. I'm ready to assist you!"
                        }
                        viewModel.voiceManager.speak(testPhrase, pitch, rate)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3326194A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Female Voice", color = NeonCyan, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Voice Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Responses",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically speak responses when enabled",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoVoice,
                        onCheckedChange = { autoVoice = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonViolet
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Multilingual Preferences
                Text(
                    text = "LANGUAGE PREFERENCE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val languages = listOf(
                    "auto" to "Auto-Detect (English / Hindi / Hinglish)",
                    "hinglish" to "Hinglish (Natural friendly)",
                    "english" to "English",
                    "hindi" to "Hindi (हिंदी)"
                )

                languages.forEach { (code, label) ->
                    val isSelected = language == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0x339D4EDD) else Color(0x11FFFFFF))
                            .border(1.dp, if (isSelected) NeonViolet else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { language = code }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, color = TextPrimary, fontSize = 13.sp)
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 3: AI Neural Core Status
                Text(
                    text = "AI NEURAL ENGINE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (hasApiKey) EmeraldGlow else NeonMagenta)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasApiKey) "Gemini 3.5 Flash Active" else "Intelligent Local Core Active",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasApiKey) {
                                "Connected via GEMINI_API_KEY. Multilingual reasoning, math solving, and coding fully enabled."
                            } else {
                                "Running with built-in intelligent engine. To unlock live Gemini 3.5 Flash reasoning, configure GEMINI_API_KEY in the AI Studio Secrets panel."
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 4: Memory Management
                Text(
                    text = "MEMORY & CONVERSATION",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.openClearConfirmation()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Chat History", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save / Done Button
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            pitch = pitch,
                            rate = rate,
                            autoVoice = autoVoice,
                            language = language
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply & Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ClearChatConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Clear Conversation?", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "Are you sure you want to reset your conversation history with MAYA? This will re-initialize the assistant memory.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("Clear All", color = NeonMagenta, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

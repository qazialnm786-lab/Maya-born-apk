package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole {
    USER,
    MAYA,
    SYSTEM
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "auto",
    val isAudioPlayed: Boolean = false
)

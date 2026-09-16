package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val autoVoice: Boolean = true,
    val speechPitch: Float = 1.25f, // Sweet female anime pitch
    val speechRate: Float = 1.0f,
    val preferredLanguage: String = "auto", // auto, hinglish, english, hindi
    val userName: String = "Friend",
    val customApiKey: String = ""
)

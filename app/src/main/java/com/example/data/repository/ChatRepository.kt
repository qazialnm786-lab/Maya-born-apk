package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MessageRole
import com.example.data.local.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val settingsFlow: Flow<UserSettingsEntity?> = chatDao.getSettingsFlow()

    suspend fun ensureWelcomeMessage() {
        val existing = chatDao.getRecentMessages(1)
        if (existing.isEmpty()) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = MessageRole.MAYA,
                    content = "Hi! I'm MAYA, your personal AI assistant. 💜\nYou can talk to me, ask questions, solve problems, study with me, or just chat. I'm ready!",
                    language = "en"
                )
            )
        }
        if (chatDao.getSettings() == null) {
            chatDao.updateSettings(UserSettingsEntity())
        }
    }

    suspend fun addMessage(role: MessageRole, content: String, language: String = "auto"): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                role = role,
                content = content,
                language = language
            )
        )
    }

    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity> {
        return chatDao.getRecentMessages(limit)
    }

    suspend fun clearHistory() {
        chatDao.clearAllMessages()
        // Re-seed welcome message
        chatDao.insertMessage(
            ChatMessageEntity(
                role = MessageRole.MAYA,
                content = "Hi! I'm MAYA, your personal AI assistant. 💜\nYou can talk to me, ask questions, solve problems, study with me, or just chat. I'm ready!",
                language = "en"
            )
        )
    }

    suspend fun updateSettings(settings: UserSettingsEntity) {
        chatDao.updateSettings(settings)
    }

    suspend fun getSettings(): UserSettingsEntity {
        return chatDao.getSettings() ?: UserSettingsEntity()
    }
}

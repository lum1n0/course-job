package com.knowledge.base.service

import com.knowledge.base.model.ChatMessage
import com.knowledge.base.repository.ChatMessageRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun saveMessage(chatMessage: ChatMessage) {
        logger.info("Attempting to save message: {}", chatMessage)
        chatMessageRepository.save(chatMessage)
        logger.info("Message saved successfully: {}", chatMessage)
    }

    fun deleteSessionMessages(sessionId: String) {
        logger.info("Deleting messages for sessionId: {}", sessionId)
        chatMessageRepository.deleteBySessionId(sessionId)
        logger.info("Messages deleted for sessionId: {}", sessionId)
    }
}
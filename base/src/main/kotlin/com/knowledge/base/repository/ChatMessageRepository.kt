package com.knowledge.base.repository

import com.knowledge.base.model.ChatMessage
import org.springframework.data.mongodb.repository.MongoRepository

interface ChatMessageRepository : MongoRepository<ChatMessage, String> {
    fun findBySessionId(sessionId: String): List<ChatMessage>
    fun deleteBySessionId(sessionId: String)
}
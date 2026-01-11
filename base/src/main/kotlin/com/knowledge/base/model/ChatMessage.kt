package com.knowledge.base.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "chat_messages")
data class ChatMessage(
    @Id
    val id: String? = null,
    val sessionId: String,
    val userId: Long,
    val message: String,
    val isFromBot: Boolean,
    val timestamp: Instant = Instant.now()
)
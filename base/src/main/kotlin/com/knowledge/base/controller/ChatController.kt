// ChatController.kt
package com.knowledge.base.controller

import com.knowledge.base.model.ChatMessage
import com.knowledge.base.service.ChatService
import com.knowledge.base.service.OllamaService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Controller
class ChatController(
    private val chatService: ChatService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val ollamaService: OllamaService
) {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)
    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()

    @MessageMapping("/chat.requestGreeting")
    fun requestGreeting(sessionInfo: Map<String, Any>) {
        val sessionId = sessionInfo["sessionId"] as? String
        if (!sessionId.isNullOrBlank()) {
            val greetingMessage = ollamaService.getGreetingMessage()
            val botMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                userId = 0L,
                message = greetingMessage,
                isFromBot = true
            )
            simpMessagingTemplate.convertAndSend("/topic/$sessionId", botMessage)
        }
    }

    @MessageMapping("/chat.sendMessage")
    fun sendMessage(chatMessage: ChatMessage) {
        logger.info("Получено сообщение в чате: {}", chatMessage)
        if (chatMessage.id != null && !processedMessageIds.add(chatMessage.id)) {
            logger.warn("Дублирующееся сообщение: {}", chatMessage.id)
            return
        }

        try {
            chatService.saveMessage(chatMessage)
            simpMessagingTemplate.convertAndSend("/topic/${chatMessage.sessionId}", chatMessage)

            val botResponseText = ollamaService.generateResponse(chatMessage.message, chatMessage.userId)
            val botMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = chatMessage.sessionId,
                userId = chatMessage.userId,
                message = botResponseText,
                isFromBot = true
            )
            chatService.saveMessage(botMessage)
            simpMessagingTemplate.convertAndSend("/topic/${chatMessage.sessionId}", botMessage)
        } catch (e: Exception) {
            logger.error("Ошибка при генерации ответа: ", e)
            val errorMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = chatMessage.sessionId,
                userId = chatMessage.userId,
                message = "Извините, произошла внутренняя ошибка при обработке вашего запроса.",
                isFromBot = true
            )
            simpMessagingTemplate.convertAndSend("/topic/${chatMessage.sessionId}", errorMessage)
        } finally {
            if (chatMessage.id != null) processedMessageIds.remove(chatMessage.id)
        }
    }
}
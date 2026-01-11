// src/main/kotlin/com/knowledge/base/controller/ChatRestController.kt
package com.knowledge.base.controller

import com.knowledge.base.service.ChatService
import com.knowledge.base.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatRestController(
    private val chatService: ChatService,
    private val jwtUtil: JwtUtil
) {

    @DeleteMapping("/session/{sessionId}")
    fun deleteSessionMessages(@PathVariable sessionId: String) {
        chatService.deleteSessionMessages(sessionId)
    }

    // Новый эндпоинт: возвращает sessionId = rtf из access-JWT
    @GetMapping("/session/current")
    fun getCurrentSessionId(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val auth = request.getHeader("Authorization").orEmpty()
        val token = auth.removePrefix("Bearer ").trim()
        if (token.isBlank()) return ResponseEntity.status(401).body(mapOf("error" to "no token"))
        val rtf = jwtUtil.extractRefreshFamily(token)
        val sessionId = rtf ?: run {
            // Фолбэк: если rtf нет (старые токены), используем subject
            jwtUtil.extractUsername(token)
        }
        return ResponseEntity.ok(mapOf("sessionId" to sessionId))
    }
}

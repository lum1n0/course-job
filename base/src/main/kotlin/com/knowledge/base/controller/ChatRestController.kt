package com.knowledge.base.controller

import com.knowledge.base.service.ChatService
import com.knowledge.base.util.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "REST API для управления чатом")
class ChatRestController(
    private val chatService: ChatService,
    private val jwtUtil: JwtUtil
) {

    @Operation(
        summary = "Удалить сообщения сессии",
        description = "Удаляет все сообщения указанной сессии чата",
        parameters = [
            Parameter(name = "sessionId", description = "ID сессии чата", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "200", description = "Сообщения успешно удалены"),
            ApiResponse(responseCode = "404", description = "Сессия не найдена")
        ]
    )
    @DeleteMapping("/session/{sessionId}")
    fun deleteSessionMessages(@Parameter(description = "ID сессии чата", required = true) @PathVariable sessionId: String) {
        chatService.deleteSessionMessages(sessionId)
    }

    @Operation(
        summary = "Получить текущий ID сессии",
        description = "Возвращает sessionId из JWT токена (refresh token family)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Session ID успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "401", description = "Токен отсутствует или недействителен")
        ]
    )
    @GetMapping("/session/current")
    fun getCurrentSessionId(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val auth = request.getHeader("Authorization").orEmpty()
        val token = auth.removePrefix("Bearer ").trim()
        if (token.isBlank()) return ResponseEntity.status(401).body(mapOf("error" to "no token"))
        val rtf = jwtUtil.extractRefreshFamily(token)
        val sessionId = rtf ?: run {
            jwtUtil.extractUsername(token)
        }
        return ResponseEntity.ok(mapOf("sessionId" to sessionId))
    }
}

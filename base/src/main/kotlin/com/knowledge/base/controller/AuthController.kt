package com.knowledge.base.controller

import com.knowledge.base.service.RefreshTokenService
import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Операции аутентификации")
class AuthController(
    private val refreshService: RefreshTokenService,
    private val userDetails: UserDetailsServiceImpl,
    private val jwtUtil: JwtUtil,
    @Value("\${app.cookie.secure:false}") private val cookieSecureDefault: Boolean
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)
    private val cookieName = "refreshToken"

    private fun isSecure(request: HttpServletRequest): Boolean {
        val xfProto = request.getHeader("X-Forwarded-Proto") ?: ""
        return cookieSecureDefault || request.isSecure || xfProto.equals("https", ignoreCase = true)
    }

    @Operation(
        summary = "Обновить токен доступа",
        description = "Обновляет access token используя refresh token из cookie",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Токен успешно обновлен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "401", description = "Refresh token отсутствует или недействителен")
        ]
    )
    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val cookie = request.cookies?.firstOrNull { it.name == cookieName }
            ?: return ResponseEntity.status(401).body(mapOf("error" to "No refresh cookie"))
        val userAgent = request.getHeader("User-Agent")
        val ip = request.remoteAddr
        val (newRaw, newRt) = refreshService.rotate(cookie.value, userAgent, ip)
        val ud = userDetails.loadUserByUsername(newRt.user.email)
        val access = jwtUtil.generateAccessToken(ud, newRt.tokenFamily)
        val setCookie = ResponseCookie.from(cookieName, newRaw)
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofSeconds(refreshService.refreshTtlSeconds()))
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, setCookie.toString())
            .body(mapOf("token" to access))
    }

    @Operation(
        summary = "Выход из системы",
        description = "Отзывает refresh token и очищает cookie",
        responses = [
            ApiResponse(responseCode = "204", description = "Успешный выход"),
            ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
        ]
    )
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        val cookie = request.cookies?.firstOrNull { it.name == cookieName }
        if (cookie != null) {
            runCatching { refreshService.revoke(cookie.value) }
        }
        val cleared = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cleared.toString())
            .build()
    }
}

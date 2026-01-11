// src/main/kotlin/com/knowledge/base/config/ArticleViewInterceptor.kt
package com.knowledge.base.config

import com.knowledge.base.service.ArticleViewService
import com.knowledge.base.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ArticleViewInterceptor(
    private val jwtUtil: JwtUtil,
    private val viewService: ArticleViewService
) : HandlerInterceptor {

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        // Интересует успешная отдача статьи
        if (request.method != "GET") return
        val path = request.requestURI ?: return
        // Матчим /api/articles/{id}
        val m = Regex("^/api/articles/(\\d+)$").find(path) ?: return
        val articleId = m.groupValues[1].toLongOrNull() ?: return

        val auth = request.getHeader("Authorization").orEmpty()
        val token = auth.removePrefix("Bearer ").trim()
        if (token.isBlank()) return

        val jti = jwtUtil.extractJti(token) ?: return

        // userId можем не знать; при необходимости получить из другого места
        viewService.recordHit(articleId, jti, null)
    }
}

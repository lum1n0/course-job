package com.knowledge.base.config

import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsServiceImpl
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val header = request.getHeader("Authorization")
            if (header != null && header.startsWith("Bearer ")) {
                val token = header.substring(7)
                val username = jwtUtil.extractUsername(token)
                if (SecurityContextHolder.getContext().authentication == null && username.isNotBlank()) {
                    val userDetails = userDetailsService.loadUserByUsername(username)
                    if (jwtUtil.validateToken(token, userDetails)) {
                        val auth = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = auth
                    }
                }
            }
        } catch (ex: Exception) {
            // Не роняем запрос, просто пропускаем как анонимный
            logger.debug("JWT filter skipped auth due to error: ${ex.message}")
        }
        filterChain.doFilter(request, response)
    }
}
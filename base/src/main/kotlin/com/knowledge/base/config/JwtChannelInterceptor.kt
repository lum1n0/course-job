// JwtChannelInterceptor.kt
package com.knowledge.base.config

import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class JwtChannelInterceptor(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsServiceImpl
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val authorizationHeader = accessor.getFirstNativeHeader("Authorization")
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                val token = authorizationHeader.substring(7)
                try {
                    val username = jwtUtil.extractUsername(token)
                    if (username != null && SecurityContextHolder.getContext().authentication == null) {
                        val userDetails = userDetailsService.loadUserByUsername(username)
                        if (jwtUtil.validateToken(token, userDetails)) {
                            val authentication = UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.authorities
                            )
                            accessor.user = authentication
                            // Критично: прокинуть аутентификацию в контекст
                            SecurityContextHolder.getContext().authentication = authentication
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        return message
    }
}

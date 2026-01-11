// WebSocketConfig.kt
package com.knowledge.base.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(private val jwtChannelInterceptor: JwtChannelInterceptor) : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    registry.addEndpoint("/chat")
        .setAllowedOriginPatterns(
            "http://localhost:4200",
            "http://pro-znania:4200",
            "https://pro-znania.llc.tagras.corp"
        )
        .withSockJS()
        .setSessionCookieNeeded(false)
        .setHeartbeatTime(25000)
}

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(jwtChannelInterceptor)
    }
}
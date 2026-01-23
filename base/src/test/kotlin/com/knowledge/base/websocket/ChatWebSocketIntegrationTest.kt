package com.knowledge.base.websocket

import com.knowledge.base.model.ChatMessage
import com.knowledge.base.service.ChatService
import com.knowledge.base.service.OllamaService
import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @MockBean
    private lateinit var chatService: ChatService

    @MockBean
    private lateinit var ollamaService: OllamaService

    @MockBean
    private lateinit var jwtUtil: JwtUtil

    @MockBean
    private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl
    
    @MockBean
    private lateinit var ldapContextSource: LdapContextSource

    private lateinit var stompClient: WebSocketStompClient

    @BeforeEach
    fun setup() {
        val transports = listOf<Transport>(WebSocketTransport(StandardWebSocketClient()))
        val sockJsClient = SockJsClient(transports)
        stompClient = WebSocketStompClient(sockJsClient)
        val converter = MappingJackson2MessageConverter()
        converter.objectMapper.registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        stompClient.messageConverter = converter

        // Мокируем валидацию JWT для разрешения подключения
        val userDetails = User.withUsername("testUser").password("pass").roles("USER").build()
        `when`(jwtUtil.extractUsername("valid_token")).thenReturn("testUser")
        `when`(userDetailsServiceImpl.loadUserByUsername("testUser")).thenReturn(userDetails)
        `when`(jwtUtil.validateToken("valid_token", userDetails)).thenReturn(true)
    }

    @Test
    fun `verify WebSocket connection and message sending`() {
        val sessionId = "test-session-id"
        val url = "ws://localhost:$port/chat"
        val stompHeaders = StompHeaders()
        stompHeaders.add("Authorization", "Bearer valid_token")

        val sessionHandler = object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                println("Connected to WebSocket")
            }
        }

        val httpHeaders: org.springframework.web.socket.WebSocketHttpHeaders? = null
        val session = stompClient.connectAsync(url, httpHeaders, stompHeaders, sessionHandler).get(5, TimeUnit.SECONDS)
        
        val blockingQueue: BlockingQueue<ChatMessage> = LinkedBlockingQueue()

        session.subscribe("/topic/$sessionId", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type = ChatMessage::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                println("HANDLE FRAME: Payload received: $payload")
                java.io.File("debug_test.log").appendText("HANDLE FRAME: ${payload}\n")
                blockingQueue.offer(payload as ChatMessage)
            }
        })

        val chatMessage = ChatMessage(
            id = "msg-1",
            sessionId = sessionId,
            userId = 1L,
            message = "Hello Bot",
            isFromBot = false
        )

        // Мокируем ответ бота
        `when`(ollamaService.generateResponse("Hello Bot", 1L)).thenReturn("Hello Human")

        session.send("/app/chat.sendMessage", chatMessage)

        // Проверяем, что получили обратно сообщение (broadcast)
        var received = blockingQueue.poll(5, TimeUnit.SECONDS)
        assertEquals("Hello Bot", received?.message)

        // Проверяем ответ бота
        // Проверяем ответ бота
        received = blockingQueue.poll(5, TimeUnit.SECONDS)
        if (received?.message?.startsWith("Извините") == true) {
             throw RuntimeException("Controller Error Received: ${received?.message}")
        }
        assertEquals("Hello Human", received?.message)
        assertEquals(true, received?.isFromBot)
    }
}

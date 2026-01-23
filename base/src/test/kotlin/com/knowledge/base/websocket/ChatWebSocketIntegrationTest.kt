package com.knowledge.base.websocket

import com.knowledge.base.model.ChatMessage
import com.knowledge.base.service.ChatService
import com.knowledge.base.service.OllamaService
import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
        
        val receivedMessages = CopyOnWriteArrayList<ChatMessage>()
        val messagesLatch = CountDownLatch(2) // Ожидаем 2 сообщения: echo и ответ бота
        val connectionError = AtomicReference<Throwable?>()
        val subscriptionConfirmed = CountDownLatch(1)

        val sessionHandler = object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                println("Connected to WebSocket, sessionId: ${session.sessionId}")
                
                // Подписываемся сразу после подключения
                session.subscribe("/topic/$sessionId", object : StompFrameHandler {
                    override fun getPayloadType(headers: StompHeaders): Type = Object::class.java

                    override fun handleFrame(headers: StompHeaders, payload: Any?) {
                        println(">>> HANDLE FRAME: Payload type: ${payload?.javaClass?.name}, payload: $payload")
                        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                            .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                            .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
                        
                        val chatMsg: ChatMessage? = when (payload) {
                            is ChatMessage -> payload
                            is ByteArray -> {
                                try {
                                    val json = String(payload, Charsets.UTF_8)
                                    println(">>> Decoded JSON: $json")
                                    mapper.readValue(json, ChatMessage::class.java)
                                } catch (e: Exception) {
                                    println(">>> Failed to parse ByteArray: ${e.message}")
                                    null
                                }
                            }
                            else -> {
                                try {
                                    mapper.convertValue(payload, ChatMessage::class.java)
                                } catch (e: Exception) {
                                    println(">>> Failed to convert: ${e.message}")
                                    null
                                }
                            }
                        }
                        
                        if (chatMsg != null) {
                            println(">>> Received ChatMessage: ${chatMsg.message}, isFromBot: ${chatMsg.isFromBot}")
                            receivedMessages.add(chatMsg)
                            messagesLatch.countDown()
                        }
                    }
                })
                subscriptionConfirmed.countDown()
            }

            override fun handleException(session: StompSession, command: StompCommand?, headers: StompHeaders, payload: ByteArray, exception: Throwable) {
                println("!!! STOMP Exception: ${exception.message}")
                exception.printStackTrace()
                connectionError.set(exception)
            }

            override fun handleTransportError(session: StompSession, exception: Throwable) {
                println("!!! Transport Error: ${exception.message}")
                exception.printStackTrace()
                connectionError.set(exception)
            }
        }

        val stompHeaders = StompHeaders()
        stompHeaders.add("Authorization", "Bearer valid_token")
        
        val httpHeaders: org.springframework.web.socket.WebSocketHttpHeaders? = null
        val session = stompClient.connectAsync(url, httpHeaders, stompHeaders, sessionHandler).get(10, TimeUnit.SECONDS)
        
        // Ждём подтверждения подписки
        assertTrue(subscriptionConfirmed.await(5, TimeUnit.SECONDS), "Subscription was not confirmed")
        
        // Существенная задержка для регистрации подписки на сервере
        Thread.sleep(2000)

        val chatMessage = ChatMessage(
            id = "msg-1",
            sessionId = sessionId,
            userId = 1L,
            message = "Hello Bot",
            isFromBot = false
        )

        // Мокируем ответ бота
        `when`(ollamaService.generateResponse("Hello Bot", 1L)).thenReturn("Hello Human")

        // Отправляем сообщение
        println("Sending message to /app/chat.sendMessage")
        session.send("/app/chat.sendMessage", chatMessage)

        // Ждём получения сообщений
        val messagesReceived = messagesLatch.await(15, TimeUnit.SECONDS)
        
        println("Messages received: $messagesReceived, count: ${receivedMessages.size}")
        receivedMessages.forEachIndexed { index, msg -> 
            println("Message $index: ${msg.message}, isFromBot: ${msg.isFromBot}")
        }

        // Проверяем, что получили сообщения
        assertTrue(receivedMessages.size >= 1, "Expected to receive at least 1 message, got ${receivedMessages.size}")
        
        val userMessage = receivedMessages.find { it.message == "Hello Bot" && !it.isFromBot }
        assertNotNull(userMessage, "Expected to receive echoed user message 'Hello Bot'")
        
        val botMessage = receivedMessages.find { it.message == "Hello Human" && it.isFromBot }
        assertNotNull(botMessage, "Expected to receive bot response 'Hello Human'")
    }
}

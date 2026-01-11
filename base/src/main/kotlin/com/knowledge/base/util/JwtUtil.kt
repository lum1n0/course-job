// src/main/kotlin/com/knowledge/base/util/JwtUtil.kt
package com.knowledge.base.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

@Component
class JwtUtil(
    @Value("\${app.jwtSecret}")
    rawSecret: String,
    @Value("\${app.auth.accessTtlMs:3600000}") // 1 час по умолчанию 3600000
    private val accessTtlMs: Long
) {
    private val logger = LoggerFactory.getLogger(JwtUtil::class.java)
    private val secret: String = rawSecret.replace("\r", "").replace("\n", "").trim()
    private val expirationTime = accessTtlMs

    private val key = run {
        try {
            val bytes = Decoders.BASE64.decode(secret)
            require(bytes.size >= 32) { "JWT secret (base64) is too short; need >=32 bytes" }
            Keys.hmacShaKeyFor(bytes)
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT secret is not valid base64; fallback to raw UTF-8 bytes.")
            val bytes = secret.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size >= 32) { "JWT secret (raw) is too short; need >=32 bytes" }
            Keys.hmacShaKeyFor(bytes)
        }
    }

    fun generateAccessToken(userDetails: UserDetails, refreshFamily: String? = null): String {
        val roles = userDetails.authorities.map { it.authority }
        val now = Date()
        val exp = Date(System.currentTimeMillis() + accessTtlMs)
        val jti = UUID.randomUUID().toString()
        val builder = Jwts.builder()
            .setSubject(userDetails.username)
            .claim("roles", roles)
            .claim("jti", jti)
            .setIssuedAt(now)
            .setExpiration(exp)
        if (!refreshFamily.isNullOrBlank()) {
            builder.claim("rtf", refreshFamily)
        }
        val token = builder.signWith(key, SignatureAlgorithm.HS512).compact()
        if (logger.isDebugEnabled) {
            logger.debug("JWT issued for '${userDetails.username}', head='${token.take(30)}...', len=${token.length}")
        }
        return token
    }

    fun generateToken(userDetails: UserDetails): String = generateAccessToken(userDetails, null)

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        return try {
            val username = extractUsername(token)
            username == userDetails.username && !isTokenExpired(token)
        } catch (ex: Exception) {
            logger.debug("JWT validation failed: ${ex.message}")
            false
        }
    }

    fun extractUsername(token: String): String = extractClaims(token).subject
    fun extractJti(token: String): String? = runCatching { extractClaims(token).get("jti", String::class.java) }.getOrNull()
    fun extractRefreshFamily(token: String): String? = runCatching { extractClaims(token).get("rtf", String::class.java) }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    fun extractRoles(token: String): List<String> =
        extractClaims(token).get("roles", List::class.java) as List<String>

    fun extractClaims(token: String): Claims =
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body

    private fun isTokenExpired(token: String): Boolean =
        extractClaims(token).expiration.before(Date())
}

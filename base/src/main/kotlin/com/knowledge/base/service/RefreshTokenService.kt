// src/main/kotlin/com/knowledge/base/service/RefreshTokenService.kt
package com.knowledge.base.service

import com.knowledge.base.model.RefreshToken
import com.knowledge.base.model.User
import com.knowledge.base.repository.RefreshTokenRepository
import com.knowledge.base.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class RefreshTokenService(
    private val refreshRepo: RefreshTokenRepository,
    private val userRepo: UserRepository
) {
    private val log = LoggerFactory.getLogger(RefreshTokenService::class.java)
    private val rng = SecureRandom()

    @Value("\${app.auth.refreshTtlDays:30}")
    private var refreshTtlDays: Int = 30

    fun refreshTtlSeconds(): Long = Duration.ofDays(refreshTtlDays.toLong()).seconds

    private fun randomUrlToken(bytes: Int = 32): String {
        val buf = ByteArray(bytes)
        rng.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun sha256Base64Url(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(s.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    @Transactional
    fun issueOnLogin(username: String, userAgent: String?, ip: String?): Pair<String, RefreshToken> {
        val user = userRepo.findByEmail(username) ?: throw IllegalStateException("User not found: $username")
        val raw = randomUrlToken(48)
        val family = randomUrlToken(16)
        val hash = sha256Base64Url(raw)
        val rt = RefreshToken(
            user = user,
            tokenHash = hash,
            tokenFamily = family,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(Duration.ofDays(refreshTtlDays.toLong())),
            revoked = false,
            userAgent = userAgent?.take(255),
            ip = ip?.take(64)
        )
        val saved = refreshRepo.save(rt)
        log.info("Issued refresh token family={} for user={}", family, username)
        return raw to saved
    }

    @Transactional
    fun rotate(oldRaw: String, userAgent: String?, ip: String?): Pair<String, RefreshToken> {
        val oldHash = sha256Base64Url(oldRaw)
        val opt = refreshRepo.findByTokenHash(oldHash)
        if (opt.isEmpty) throw IllegalArgumentException("Invalid refresh token")
        val current = opt.get()

        // Reuse detection
        if (current.revoked) {
            // Replay: revoke entire family
            revokeFamily(current.user, current.tokenFamily)
            throw IllegalStateException("Refresh token reused; family revoked")
        }
        if (current.expiresAt.isBefore(Instant.now())) {
            current.revoked = true
            refreshRepo.save(current)
            throw IllegalStateException("Refresh token expired")
        }

        // Create new token in same family, revoke old
        val newRaw = randomUrlToken(48)
        val newHash = sha256Base64Url(newRaw)
        val replacement = RefreshToken(
            user = current.user,
            tokenHash = newHash,
            tokenFamily = current.tokenFamily,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(Duration.ofDays(refreshTtlDays.toLong())),
            revoked = false,
            userAgent = userAgent?.take(255),
            ip = ip?.take(64)
        )
        val saved = refreshRepo.save(replacement)
        current.revoked = true
        current.replacedBy = newHash
        refreshRepo.save(current)

        return newRaw to saved
    }

    @Transactional
    fun revoke(raw: String) {
        val hash = sha256Base64Url(raw)
        refreshRepo.findByTokenHash(hash).ifPresent {
            it.revoked = true
            refreshRepo.save(it)
        }
    }

    @Transactional
    fun revokeFamily(user: User, family: String) {
        val all = refreshRepo.findAllByUserAndTokenFamily(user, family)
        all.forEach { it.revoked = true }
        refreshRepo.saveAll(all)
        log.warn("Revoked refresh family={} for user={}", family, user.email)
    }

    @Transactional
    fun cleanupExpired(): Long = refreshRepo.deleteAllByExpiresAtBefore(Instant.now())
}

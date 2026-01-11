// src/main/kotlin/com/knowledge/base/repository/RefreshTokenRepository.kt
package com.knowledge.base.repository

import com.knowledge.base.model.RefreshToken
import com.knowledge.base.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findAllByUserAndTokenFamily(user: User, tokenFamily: String): List<RefreshToken>
    fun deleteAllByExpiresAtBefore(ts: Instant): Long
}

// src/main/kotlin/com/knowledge/base/model/RefreshToken.kt
package com.knowledge.base.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "ix_refresh_token_hash", columnList = "tokenHash", unique = true),
    Index(name = "ix_refresh_token_family", columnList = "tokenFamily"),
    Index(name = "ix_refresh_token_user", columnList = "user_id")
])
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true, length = 128)
    val tokenHash: String,

    @Column(nullable = false, length = 64)
    val tokenFamily: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false,

    @Column(length = 128)
    var replacedBy: String? = null,

    @Column(length = 255)
    var userAgent: String? = null,

    @Column(length = 64)
    var ip: String? = null
)

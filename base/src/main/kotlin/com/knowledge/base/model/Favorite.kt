// src/main/kotlin/com/knowledge/base/model/Favorite.kt
package com.knowledge.base.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_favorites")
data class Favorite(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "favorite_seq")
    @SequenceGenerator(name = "favorite_seq", sequenceName = "favorite_sequence", allocationSize = 1)
    @Column(name = "favorite_id", nullable = false)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    val article: Article,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

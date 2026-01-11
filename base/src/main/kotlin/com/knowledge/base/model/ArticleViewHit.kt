// src/main/kotlin/com/knowledge/base/model/ArticleViewHit.kt
package com.knowledge.base.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "article_view_hits",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_article_jti", columnNames = ["article_id", "accessJti"])
    ],
    indexes = [
        Index(name = "ix_avh_article", columnList = "article_id"),
        Index(name = "ix_avh_created", columnList = "createdAt")
    ]
)
data class ArticleViewHit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    val article: Article,

    @Column(nullable = false, length = 40)
    val accessJti: String,

    @Column(nullable = true)
    val userId: Long? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

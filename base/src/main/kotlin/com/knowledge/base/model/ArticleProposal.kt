// src/main/kotlin/com/knowledge/base/model/ArticleProposal.kt
package com.knowledge.base.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "article_proposal")
data class ArticleProposal(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "article_proposal_seq")
    @SequenceGenerator(name = "article_proposal_seq", sequenceName = "article_proposal_sequence", allocationSize = 1)
    val id: Long = 0,

    // null -> создание новой статьи, иначе обновление существующей
    @Column(name = "article_id")
    val articleId: Long? = null,

    // Новое поле: ID статьи, которая была создана или обновлена в результате одобрения
    @Column(name = "final_article_id")
    val finalArticleId: Long? = null, // Может быть null до одобрения

    @Column(nullable = false, name = "title")
    val title: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, name = "description", columnDefinition = "jsonb")
    val description: JsonNode?,

    @Column(nullable = false, name = "category_id")
    val categoryId: Long,

    @Column(name = "video_path")
    val videoPath: List<String>? = null,

    @Column(name = "file_path")
    val filePath: List<String>? = null,

    // автор
    @Column(nullable = false, name = "author_id")
    val authorId: Long,

    @Column(nullable = false, name = "author_email")
    val authorEmail: String,

    @Column(name = "author_name")
    val authorName: String? = null,

    // модерация
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    val status: ModerationStatus = ModerationStatus.PENDING,

    @Column(name = "reviewed_by_id")
    val reviewedById: Long? = null,

    @Column(name = "reviewed_by_email")
    val reviewedByEmail: String? = null,

    @Column(name = "reviewed_by_name")
    val reviewedByName: String? = null,

    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,

    @Column(name = "reject_reason")
    val rejectReason: String? = null,

    // CREATE или UPDATE
    @Column(nullable = false, name = "action")
    val action: String,

    @Column(nullable = false, name = "created_at")
    val createdAt: Instant = Instant.now()
)

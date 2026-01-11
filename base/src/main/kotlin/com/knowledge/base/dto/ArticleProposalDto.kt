// src/main/kotlin/com/knowledge/base/dto/ArticleProposalDto.kt
package com.knowledge.base.dto

import com.fasterxml.jackson.databind.JsonNode
import com.knowledge.base.model.ModerationStatus
import java.time.Instant

data class ArticleProposalDto(
    val id: Long = 0,
    val articleId: Long?, // ID статьи, которую обновляют (null для CREATE)
    val finalArticleId: Long?, // Новое поле: ID статьи, которая будет/уже создана в результате
    val title: String,
    val description: JsonNode?,
    val categoryId: Long,
    val videoPath: List<String>?,
    val filePath: List<String>?,

    val authorId: Long,
    val authorEmail: String,
    val authorName: String?,

    val status: ModerationStatus,
    val reviewedById: Long?,
    val reviewedByEmail: String?,
    val reviewedByName: String?,
    val reviewedAt: Instant?,
    val rejectReason: String?,

    val action: String,
    val createdAt: Instant
)

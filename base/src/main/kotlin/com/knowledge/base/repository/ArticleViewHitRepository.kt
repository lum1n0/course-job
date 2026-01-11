// src/main/kotlin/com/knowledge/base/repository/ArticleViewHitRepository.kt
package com.knowledge.base.repository

import com.knowledge.base.model.ArticleViewHit
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface ArticleViewHitRepository : JpaRepository<ArticleViewHit, Long> {
    fun existsByArticleIdAndAccessJti(articleId: Long, accessJti: String): Boolean
    fun countByArticleId(articleId: Long): Long
    fun deleteByArticleId(articleId: Long)
    fun countByArticleIdAndCreatedAtAfter(articleId: Long, after: Instant): Long
}


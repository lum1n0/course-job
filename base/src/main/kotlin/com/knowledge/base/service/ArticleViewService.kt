// src/main/kotlin/com/knowledge/base/service/ArticleViewService.kt
package com.knowledge.base.service

import com.knowledge.base.model.ArticleViewHit
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.ArticleViewHitRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class ArticleViewService(
    private val articleRepo: ArticleRepository,
    private val hitRepo: ArticleViewHitRepository
) {
    private val log = LoggerFactory.getLogger(ArticleViewService::class.java)

    @Transactional
    fun recordHit(articleId: Long, accessJti: String, userId: Long?) {
        if (accessJti.isBlank()) return
        if (hitRepo.existsByArticleIdAndAccessJti(articleId, accessJti)) return
        val article = articleRepo.findById(articleId).orElse(null) ?: return
        hitRepo.save(ArticleViewHit(article = article, accessJti = accessJti, userId = userId))
        log.debug("Recorded view hit articleId={} jti={}", articleId, accessJti)
    }
    @Transactional(readOnly = true)
    fun getTotalViews(articleId: Long): Long = hitRepo.countByArticleId(articleId)

    @Transactional(readOnly = true)
    fun getViewsLast24h(articleId: Long): Long =
        hitRepo.countByArticleIdAndCreatedAtAfter(articleId, Instant.now().minus(Duration.ofHours(24)))
}
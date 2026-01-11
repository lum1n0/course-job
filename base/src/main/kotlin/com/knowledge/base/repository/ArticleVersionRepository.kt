package com.knowledge.base.repository

import com.knowledge.base.model.ArticleVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ArticleVersionRepository : JpaRepository<ArticleVersion, Long> {
    fun findByArticleIdOrderByVersionDesc(articleId: Long): List<ArticleVersion>
    fun findTopByArticleIdOrderByVersionDesc(articleId: Long): ArticleVersion?
    fun findByArticleIdAndVersion(articleId: Long, version: Int): Optional<ArticleVersion>
    fun existsByArticleIdAndVersion(articleId: Long, version: Int): Boolean
    fun deleteByArticleIdAndVersion(articleId: Long, version: Int)
}

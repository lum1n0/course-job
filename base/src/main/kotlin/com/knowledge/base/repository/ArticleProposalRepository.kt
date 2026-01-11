// src/main/kotlin/com/knowledge/base/repository/ArticleProposalRepository.kt
package com.knowledge.base.repository

import com.knowledge.base.model.ArticleProposal
import com.knowledge.base.model.ModerationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleProposalRepository : JpaRepository<ArticleProposal, Long> {
    fun findAllByStatusOrderByCreatedAtDesc(status: ModerationStatus): List<ArticleProposal>
    fun findAllByAuthorIdOrderByCreatedAtDesc(authorId: Long): List<ArticleProposal>
}

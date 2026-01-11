package com.knowledge.base.repository

import com.knowledge.base.model.Article
import com.knowledge.base.model.Feedback
import com.knowledge.base.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository: JpaRepository<Feedback, Long> {
    fun findFeedbackByUserEmail(user: User): List<Feedback>
    fun findFeedbackByArticleId(article: Article): List<Feedback>
    fun findByIsAnsweredFalse(pageable: Pageable): Page<Feedback>
}
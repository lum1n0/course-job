// src/main/kotlin/com/knowledge/base/repository/FavoriteRepository.kt
package com.knowledge.base.repository

import com.knowledge.base.model.Favorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FavoriteRepository : JpaRepository<Favorite, Long> {
    fun findByUserIdAndArticleId(userId: Long, articleId: Long): Favorite?
    fun existsByUserIdAndArticleId(userId: Long, articleId: Long): Boolean
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Favorite>
}

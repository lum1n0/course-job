// src/main/kotlin/com/knowledge/base/service/FavoriteService.kt
package com.knowledge.base.service

import com.knowledge.base.dto.FavoriteDto
import com.knowledge.base.mapper.ArticleMapper
import com.knowledge.base.model.Favorite
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.FavoriteRepository
import com.knowledge.base.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val userRepository: UserRepository,
    private val articleRepository: ArticleRepository,
    private val articleMapper: ArticleMapper
) {

    fun add(email: String, articleId: Long): FavoriteDto {
        val user = userRepository.findByEmail(email) ?: throw AccessDeniedException("Forbidden")
        val article = articleRepository.findById(articleId)
            .orElseThrow { IllegalArgumentException("Статья не найдена") }
        val existing = favoriteRepository.findByUserIdAndArticleId(user.id, articleId)
        if (existing != null) return toDto(existing)
        val saved = favoriteRepository.save(Favorite(user = user, article = article))
        return toDto(saved)
    }

    fun remove(email: String, articleId: Long) {
        val user = userRepository.findByEmail(email) ?: return
        favoriteRepository.findByUserIdAndArticleId(user.id, articleId)?.let { favoriteRepository.delete(it) }
    }

    @Transactional(readOnly = true)
    fun list(email: String): List<FavoriteDto> {
        val user = userRepository.findByEmail(email) ?: throw AccessDeniedException("Forbidden")
        return favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(user.id).map { toDto(it) }
    }

    private fun toDto(f: Favorite): FavoriteDto =
        FavoriteDto(
            id = f.id,
            article = articleMapper.toDto(f.article),
            createdAt = f.createdAt.toEpochMilli()
        )
}

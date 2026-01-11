package com.knowledge.base.service

import com.knowledge.base.dto.FeedbackDto
import com.knowledge.base.mapper.FeedbackMapper
import com.knowledge.base.model.Article
import com.knowledge.base.model.Feedback
import com.knowledge.base.model.User
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.FeedbackRepository
import com.knowledge.base.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val feedbackMapper: FeedbackMapper,
    private val userRepository: UserRepository,
    private val articleRepository: ArticleRepository
) {

    // FeedbackService.kt
    fun addFeedbackRequest(feedbackDto: FeedbackDto, userEmailToToken: String): Feedback {
        val currentUser: User = userRepository.findByEmail(userEmailToToken)
            ?: throw UsernameNotFoundException("Пользователь с email $userEmailToToken не найден")

        val currentArticle: Article = when {
            feedbackDto.articleId != null -> {
                articleRepository.findById(feedbackDto.articleId).orElseThrow {
                    EntityNotFoundException("Статья с ID ${feedbackDto.articleId} не найдена")
                }
            }
            feedbackDto.articleTitle.isNotBlank() -> {
                // Временная обратная совместимость, но лучше не использовать заголовок
                articleRepository.findByTitle(feedbackDto.articleTitle)
                    ?: throw EntityNotFoundException("Статья с заголовком ${feedbackDto.articleTitle} не найдена")
            }
            else -> throw IllegalArgumentException("Не указан articleId или articleTitle")
        }

        val feedbackEntity = Feedback(
            title = feedbackDto.title,
            description = feedbackDto.description,
            user = currentUser,
            article = currentArticle,
            isAnswered = feedbackDto.isAnswered
        )

        return feedbackRepository.save(feedbackEntity)
    }

    fun getAllFeedback(pageable: Pageable): Page<FeedbackDto> {
        // Сортировка: неотвеченные сначала, отвеченные внизу
        val sortedPageable = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(Sort.Direction.ASC, "isAnswered").and(Sort.by(Sort.Direction.DESC, "id"))
        )

        val pageFeedback: Page<Feedback> = feedbackRepository.findAll(sortedPageable)
        return pageFeedback.map { feedbackMapper.toDto(it) }
    }

    fun updateFeedbackStatus(id: Long, isAnswered: Boolean): Feedback {
        val feedback = feedbackRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Отзыв с ID $id не найден") }

        val updatedFeedback = feedback.copy(isAnswered = isAnswered)
        return feedbackRepository.save(updatedFeedback)
    }

    fun findFeedbackByUserEmail(user: User): List<FeedbackDto> {
        val foundUser: User? = userRepository.findByEmail(user.email)
        if (foundUser == null) return emptyList()
        val feedbackEntity: List<Feedback> = feedbackRepository.findFeedbackByUserEmail(foundUser)
        return feedbackEntity.map { feedbackMapper.toDto(it) }
    }

    fun findFeedbackByArticle(article: Article): List<FeedbackDto> {
        val foundArticle = articleRepository.findById(article.id).orElse(null) ?: return emptyList()
        val feedbackEntity: List<Feedback> = feedbackRepository.findFeedbackByArticleId(foundArticle)
        return feedbackEntity.map { feedbackMapper.toDto(it) }
    }

    fun getAllFeedbackIsAnsweredFalse(pageable: Pageable): Page<FeedbackDto> {
        val pageFeedback: Page<Feedback> = feedbackRepository.findByIsAnsweredFalse(pageable)
        return pageFeedback.map { feedbackMapper.toDto(it) }
    }
}

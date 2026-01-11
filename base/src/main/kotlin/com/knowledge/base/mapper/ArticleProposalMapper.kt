// src/main/kotlin/com/knowledge/base/mapper/ArticleProposalMapper.kt
package com.knowledge.base.mapper

import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.model.ArticleProposal
import org.springframework.stereotype.Component

// Убираем зависимость от ModelMapper
@Component
class ArticleProposalMapper {

    fun toDto(proposal: ArticleProposal): ArticleProposalDto {
        // Явно создаем DTO, передавая все поля
        return ArticleProposalDto(
            id = proposal.id,
            articleId = proposal.articleId,
            finalArticleId = proposal.finalArticleId, // <-- Новое поле
            title = proposal.title,
            description = proposal.description,
            categoryId = proposal.categoryId,
            videoPath = proposal.videoPath,
            filePath = proposal.filePath,
            authorId = proposal.authorId,
            authorEmail = proposal.authorEmail,
            authorName = proposal.authorName,
            status = proposal.status,
            reviewedById = proposal.reviewedById,
            reviewedByEmail = proposal.reviewedByEmail,
            reviewedByName = proposal.reviewedByName,
            reviewedAt = proposal.reviewedAt,
            rejectReason = proposal.rejectReason,
            action = proposal.action,
            createdAt = proposal.createdAt
        )
    }

    // Опционально: метод для маппинга DTO в Entity, если понадобится
    fun toEntity(dto: ArticleProposalDto): ArticleProposal {
        return ArticleProposal(
            id = dto.id,
            articleId = dto.articleId,
            finalArticleId = dto.finalArticleId, // <-- Новое поле
            title = dto.title,
            description = dto.description,
            categoryId = dto.categoryId,
            videoPath = dto.videoPath,
            filePath = dto.filePath,
            authorId = dto.authorId,
            authorEmail = dto.authorEmail,
            authorName = dto.authorName,
            status = dto.status,
            reviewedById = dto.reviewedById,
            reviewedByEmail = dto.reviewedByEmail,
            reviewedByName = dto.reviewedByName,
            reviewedAt = dto.reviewedAt,
            rejectReason = dto.rejectReason,
            action = dto.action,
            createdAt = dto.createdAt
        )
    }
}
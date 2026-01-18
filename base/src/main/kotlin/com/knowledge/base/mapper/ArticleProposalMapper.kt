package com.knowledge.base.mapper

import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.model.Article
import com.knowledge.base.model.ArticleProposal
import org.springframework.stereotype.Component

@Component
class ArticleProposalMapper {

    fun toDto(proposal: ArticleProposal): ArticleProposalDto {
        return ArticleProposalDto(
            id = proposal.id,
            // ИСПРАВЛЕНИЕ 1: Достаем ID из связанного объекта (безопасный вызов ?.)
            articleId = proposal.article?.id,
            finalArticleId = proposal.finalArticleId,
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

    // ИСПРАВЛЕНИЕ 2: Изменили сигнатуру. Теперь принимаем article отдельно.
    // Если articleId в DTO null, то и article передаем null.
    fun toEntity(dto: ArticleProposalDto, article: Article? = null): ArticleProposal {
        return ArticleProposal(
            id = dto.id,
            // Передаем объект сущности
            article = article,
            finalArticleId = dto.finalArticleId,
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

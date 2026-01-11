package com.knowledge.base.mapper

import com.knowledge.base.dto.ArticleVersionDto
import com.knowledge.base.model.ArticleVersion
import org.springframework.stereotype.Component

@Component
class ArticleVersionMapper {
    fun toDto(av: ArticleVersion): ArticleVersionDto =
        ArticleVersionDto(
            id = av.id,
            articleId = av.articleId,
            version = av.version,
            title = av.title,
            description = av.description,
            categoryId = av.categoryId,
            videoPath = av.videoPath,
            filePath = av.filePath,
            editedById = av.editedById,
            editedByEmail = av.editedByEmail,
            editedByName = av.editedByName,
            editedAt = av.editedAt,
            changeSummary = av.changeSummary
        )
}

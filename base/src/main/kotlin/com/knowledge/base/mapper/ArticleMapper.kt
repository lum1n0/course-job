
package com.knowledge.base.mapper

import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.model.Article
import org.modelmapper.ModelMapper
import org.springframework.stereotype.Component

@Component
class ArticleMapper(
    private val modelMapper: ModelMapper,
    private val categoryMapper: CategoryMapper
) {

    fun toDto(article: Article): ArticleDto {
        val articleDto = modelMapper.map(article, ArticleDto::class.java)
        return articleDto.copy(
            categoryDto = categoryMapper.toDto(article.category)
        )
    }

    fun toEntity(dto: ArticleDto): Article {
        val article = modelMapper.map(dto, Article::class.java)
        return article.copy(
            category = categoryMapper.toEntity(dto.categoryDto)
        )
    }
}
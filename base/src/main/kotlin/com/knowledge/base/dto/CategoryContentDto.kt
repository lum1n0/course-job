package com.knowledge.base.dto

data class CategoryContentDto(
    val categories: List<CategoryDto> = emptyList(),
    val articles: List<ArticleDto> = emptyList()
)

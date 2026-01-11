// src/main/kotlin/com/knowledge/base/dto/FavoriteDto.kt
package com.knowledge.base.dto

data class FavoriteDto(
    val id: Long,
    val article: ArticleDto,
    val createdAt: Long
)

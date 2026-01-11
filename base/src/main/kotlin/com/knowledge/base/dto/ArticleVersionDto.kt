package com.knowledge.base.dto

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

data class ArticleVersionDto(
    val id: Long = 0,
    val articleId: Long,
    val version: Int,
    val title: String,
    val description: JsonNode?,
    val categoryId: Long,
    val videoPath: List<String>?,
    val filePath: List<String>?,
    val editedById: Long?,
    val editedByEmail: String?,
    val editedByName: String?,
    val editedAt: Instant,
    val changeSummary: String?
)

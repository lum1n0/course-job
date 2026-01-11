package com.knowledge.base.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode

data class ArticleDto(
    val id: Long = 0,
    val title: String = "",
    val description: JsonNode? = NullNode.instance,
    val categoryDto: CategoryDto = CategoryDto(),
    val isDelete: Boolean = false,
    val videoPath: List<String>? = emptyList(),
    val filePath: List<String>? = emptyList(),
)

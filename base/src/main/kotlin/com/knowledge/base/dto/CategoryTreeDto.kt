package com.knowledge.base.dto

data class CategoryTreeDto(
    val id: Long,
    val description: String,
    val iconPath: String,
    val isDelete: Boolean,
    val parentId: Long?,
    val children: List<CategoryTreeDto>
)

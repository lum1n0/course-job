package com.knowledge.base.dto

data class CategoryDto(
    val id: Long = 0,
    val description: String = "",
    val iconPath: String = "",
    val isDelete: Boolean = false,
    val parentId: Long? = null,
    val accessRolesDto: List<AccessRoleDto> = emptyList()
)

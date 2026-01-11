package com.knowledge.base.dto

data class AccessRoleDto(
    val id: Long = 0,
    val title: String = "",
    val translitTitle: String = "",
    val userDto: List<UserDto>? = null,
    val categoryDto: List<CategoryDto>? = null
)
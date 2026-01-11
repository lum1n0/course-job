package com.knowledge.base.dto

data class UserDto(
    val id: Long = 0,
    val firstName: String? = null,
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    var roleDto: RoleDto = RoleDto(),
    var accessRolesDto: List<AccessRoleDto> = emptyList(),
    val isDelete: Boolean = false,
    // --- НОВОЕ ПОЛЕ ---
    val isFromLdap: Boolean = false
)

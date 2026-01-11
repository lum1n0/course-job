package com.knowledge.base.dto

data class ModeratorPermissionDto(
    val id: Long,
    val moderatorId: Long,
    val accessRoleId: Long,
    val enabled: Boolean
)

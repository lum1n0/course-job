package com.knowledge.base.dto

data class WriterPermissionDto(
    val id: Long = 0,
    val writerId: Long,
    val accessRoleId: Long,
    val enabled: Boolean = true
)


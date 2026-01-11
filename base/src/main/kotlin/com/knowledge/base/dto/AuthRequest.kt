package com.knowledge.base.dto

data class AuthRequest(
    val email: String,
    val password: String,
    val roleDto: Any? =null,
)

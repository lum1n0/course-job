package com.knowledge.base.dto

// FeedbackDto.kt
data class FeedbackDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val userEmail: String = "",   // контроллер всё равно берёт email из authentication
    val articleTitle: String = "", // оставлено для обратной совместимости
    val articleId: Long? = null,   // ← добавить это поле
    val isAnswered: Boolean = false
)

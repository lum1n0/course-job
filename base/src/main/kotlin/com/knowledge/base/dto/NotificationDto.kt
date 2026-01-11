package com.knowledge.base.dto

import com.knowledge.base.model.NotificationType
import java.time.Instant

data class NotificationDto(
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val recipientId: Long,
    val recipientEmail: String,
    val recipientName: String,
    val isRead: Boolean = false,
    val articleId: Long? = null,
    val articleProposalId: Long? = null,
    val senderName: String? = null,
    val createdAt: Instant = Instant.now()
)

data class CustomNotificationRequest(
    val title: String,
    val message: String,
    val recipientType: RecipientType,
    val recipientIds: List<Long>? = null,
    val roleId: Long? = null,
    val accessRoleId: Long? = null
)

enum class RecipientType {
    SPECIFIC_USERS,    // Конкретные пользователи
    BY_ROLE,           // По системной роли (ADMIN, MODERATOR, WRITER, USER)
    BY_ACCESS_ROLE     // По роли доступа
}

data class NotificationStatsDto(
    val totalNotifications: Long,
    val unreadCount: Long
)

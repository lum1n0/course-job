package com.knowledge.base.mapper

import com.knowledge.base.dto.NotificationDto
import com.knowledge.base.model.Notification
import org.springframework.stereotype.Component

@Component
class NotificationMapper {

    fun toDto(notification: Notification): NotificationDto {
        return NotificationDto(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            type = notification.type,
            recipientId = notification.recipient.id,
            recipientEmail = notification.recipient.email,
            recipientName = "${notification.recipient.firstName ?: ""} ${notification.recipient.lastName}".trim(),
            isRead = notification.isRead,
            articleId = notification.articleId,
            articleProposalId = notification.articleProposalId,
            senderName = notification.sender?.let { "${it.firstName ?: ""} ${it.lastName}".trim() },
            createdAt = notification.createdAt
        )
    }
}

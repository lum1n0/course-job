package com.knowledge.base.service

import com.knowledge.base.dto.CustomNotificationRequest
import com.knowledge.base.dto.NotificationDto
import com.knowledge.base.dto.NotificationStatsDto
import com.knowledge.base.dto.RecipientType
import com.knowledge.base.mapper.NotificationMapper
import com.knowledge.base.model.*
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.NotificationRepository
import com.knowledge.base.repository.RoleRepository
import com.knowledge.base.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val notificationMapper: NotificationMapper
) {

    /**
     * Уведомление о новой статье - получают пользователи с доступом к категории
     */
    @Transactional
    fun notifyAboutNewArticle(article: Article, author: User) {
        val category = article.category
        val recipients = getUsersWithAccessToCategory(category)
            .filter { it.id != author.id } // Исключаем самого автора

        recipients.forEach { recipient ->
            val notification = Notification(
                title = "Новая статья: ${article.title}",
                message = "Опубликована новая статья \"${article.title}\" в категории \"${category.description}\".",
                type = NotificationType.NEW_ARTICLE,
                recipient = recipient,
                articleId = article.id,
                sender = author
            )
            notificationRepository.save(notification)
        }
    }

    /**
     * Уведомление об обновлении статьи - получают пользователи с доступом к категории
     */
    @Transactional
    fun notifyAboutArticleUpdate(article: Article, editor: User) {
        val category = article.category
        val recipients = getUsersWithAccessToCategory(category)
            .filter { it.id != editor.id } // Исключаем самого редактора

        recipients.forEach { recipient ->
            val notification = Notification(
                title = "Обновление статьи: ${article.title}",
                message = "Статья \"${article.title}\" была обновлена в категории \"${category.description}\".",
                type = NotificationType.ARTICLE_UPDATED,
                recipient = recipient,
                articleId = article.id,
                sender = editor
            )
            notificationRepository.save(notification)
        }
    }

    /**
     * Уведомление автору о том, что его статья прошла модерацию
     */
    @Transactional
    fun notifyProposalApproved(proposal: ArticleProposal, moderator: User, publishedArticle: Article) {
        val author = userRepository.findById(proposal.authorId).orElse(null) ?: return

        val notification = Notification(
            title = "Статья одобрена",
            message = "Ваша статья \"${proposal.title}\" прошла модерацию и была опубликована.",
            type = NotificationType.PROPOSAL_APPROVED,
            recipient = author,
            articleId = publishedArticle.id,
            articleProposalId = proposal.id,
            sender = moderator
        )
        notificationRepository.save(notification)
    }

    /**
     * Уведомление автору о том, что его статья отклонена
     */
    @Transactional
    fun notifyProposalRejected(proposal: ArticleProposal, moderator: User, reason: String?) {
        val author = userRepository.findById(proposal.authorId).orElse(null) ?: return

        val reasonText = if (reason.isNullOrBlank()) "" else "\n\nПричина отклонения: $reason"

        val notification = Notification(
            title = "Статья отклонена",
            message = "Ваша статья \"${proposal.title}\" была отклонена модератором.$reasonText",
            type = NotificationType.PROPOSAL_REJECTED,
            recipient = author,
            articleProposalId = proposal.id,
            sender = moderator
        )
        notificationRepository.save(notification)
    }

    /**
     * Ручная рассылка от администратора/модератора
     */
    @Transactional
    fun sendCustomNotification(senderEmail: String, request: CustomNotificationRequest): Int {
        val sender = userRepository.findByEmail(senderEmail)
            ?: throw AccessDeniedException("Forbidden")

        // Проверка прав доступа (только ADMIN или MODERATOR могут отправлять ручные рассылки)
        if (sender.role.title !in listOf("ADMIN", "MODERATOR")) {
            throw AccessDeniedException("Only administrators and moderators can send notifications")
        }

        val recipients = when (request.recipientType) {
            RecipientType.SPECIFIC_USERS -> {
                request.recipientIds?.let { ids ->
                    userRepository.findAllById(ids).filter { !it.isDelete }
                } ?: emptyList()
            }
            RecipientType.BY_ROLE -> {
                request.roleId?.let { roleId ->
                    val role = roleRepository.findById(roleId).orElse(null)
                        ?: throw IllegalArgumentException("Role not found")
                    userRepository.findAll().filter { it.role.id == role.id && !it.isDelete }
                } ?: emptyList()
            }
            RecipientType.BY_ACCESS_ROLE -> {
                request.accessRoleId?.let { accessRoleId ->
                    val accessRole = accessRoleRepository.findById(accessRoleId).orElse(null)
                        ?: throw IllegalArgumentException("Access role not found")
                    userRepository.findAll().filter { user ->
                        !user.isDelete && user.accessRoles.any { it.id == accessRole.id }
                    }
                } ?: emptyList()
            }
        }

        recipients.forEach { recipient ->
            val notification = Notification(
                title = request.title,
                message = request.message,
                type = NotificationType.CUSTOM_MESSAGE,
                recipient = recipient,
                sender = sender
            )
            notificationRepository.save(notification)
        }

        return recipients.size
    }

    /**
     * Получить все уведомления пользователя с пагинацией
     */
    @Transactional(readOnly = true)
    fun getUserNotifications(userEmail: String, pageable: Pageable): Page<NotificationDto> {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
            .map { notificationMapper.toDto(it) }
    }

    /**
     * Получить непрочитанные уведомления пользователя
     */
    @Transactional(readOnly = true)
    fun getUnreadNotifications(userEmail: String): List<NotificationDto> {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        return notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user)
            .map { notificationMapper.toDto(it) }
    }

    /**
     * Получить статистику уведомлений
     */
    @Transactional(readOnly = true)
    fun getNotificationStats(userEmail: String): NotificationStatsDto {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        val total = notificationRepository.count()
        val unread = notificationRepository.countByRecipientAndIsReadFalse(user)

        return NotificationStatsDto(
            totalNotifications = total,
            unreadCount = unread
        )
    }

    /**
     * Отметить уведомление как прочитанное
     */
    @Transactional
    fun markAsRead(userEmail: String, notificationId: Long) {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        notificationRepository.markAsReadByIdAndUserId(notificationId, user.id)
    }

    /**
     * Отметить все уведомления пользователя как прочитанные
     */
    @Transactional
    fun markAllAsRead(userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        notificationRepository.markAllAsReadByUserId(user.id)
    }

    /**
     * Удалить уведомление
     */
    @Transactional
    fun deleteNotification(userEmail: String, notificationId: Long) {
        val user = userRepository.findByEmail(userEmail)
            ?: throw AccessDeniedException("Forbidden")

        val notification = notificationRepository.findById(notificationId).orElse(null)
            ?: throw IllegalArgumentException("Notification not found")

        if (notification.recipient.id != user.id) {
            throw AccessDeniedException("You can only delete your own notifications")
        }

        notificationRepository.delete(notification)
    }

    /**
     * Вспомогательная функция: получить всех пользователей с доступом к категории
     */
    private fun getUsersWithAccessToCategory(category: Category): List<User> {
        val categoryAccessRoleIds = category.accessRoles.map { it.id }.toSet()

        return userRepository.findAll()
            .filter { user ->
                !user.isDelete && user.accessRoles.any { it.id in categoryAccessRoleIds }
            }
    }
}

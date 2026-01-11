package com.knowledge.base.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_sequence", allocationSize = 1)
    @Column(name = "notification_id")
    val id: Long = 0,

    @Column(nullable = false, name = "title")
    val title: String,

    @Column(nullable = false, name = "message", columnDefinition = "TEXT")
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type")
    val type: NotificationType,

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @Column(nullable = false, name = "is_read")
    val isRead: Boolean = false,

    @Column(name = "article_id")
    val articleId: Long? = null,

    @Column(name = "article_proposal_id")
    val articleProposalId: Long? = null,

    @ManyToOne
    @JoinColumn(name = "sender_id")
    val sender: User? = null,

    @Column(nullable = false, name = "created_at")
    val createdAt: Instant = Instant.now()
)

enum class NotificationType {
    NEW_ARTICLE,           // Новая статья опубликована
    ARTICLE_UPDATED,       // Статья обновлена
    PROPOSAL_APPROVED,     // Статья прошла модерацию
    PROPOSAL_REJECTED,     // Статья отклонена
    CUSTOM_MESSAGE         // Ручное сообщение от администратора
}

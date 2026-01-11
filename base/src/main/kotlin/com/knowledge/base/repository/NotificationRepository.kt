package com.knowledge.base.repository

import com.knowledge.base.model.Notification
import com.knowledge.base.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByRecipientOrderByCreatedAtDesc(recipient: User, pageable: Pageable): Page<Notification>

    fun findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient: User): List<Notification>

    fun countByRecipientAndIsReadFalse(recipient: User): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(@Param("userId") userId: Long)

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :userId")
    fun markAsReadByIdAndUserId(@Param("notificationId") notificationId: Long, @Param("userId") userId: Long)
}

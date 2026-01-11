package com.knowledge.base.controller

import com.knowledge.base.dto.CustomNotificationRequest
import com.knowledge.base.dto.NotificationDto
import com.knowledge.base.dto.NotificationStatsDto
import com.knowledge.base.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    /**
     * Получить все уведомления текущего пользователя с пагинацией
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    fun getUserNotifications(
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<NotificationDto>> {
        val notifications = notificationService.getUserNotifications(authentication.name, pageable)
        return ResponseEntity.ok(notifications)
    }

    /**
     * Получить непрочитанные уведомления
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    fun getUnreadNotifications(authentication: Authentication): ResponseEntity<List<NotificationDto>> {
        val notifications = notificationService.getUnreadNotifications(authentication.name)
        return ResponseEntity.ok(notifications)
    }

    /**
     * Получить статистику уведомлений
     * GET /api/notifications/stats
     */
    @GetMapping("/stats")
    fun getNotificationStats(authentication: Authentication): ResponseEntity<NotificationStatsDto> {
        val stats = notificationService.getNotificationStats(authentication.name)
        return ResponseEntity.ok(stats)
    }

    /**
     * Отметить уведомление как прочитанное
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    fun markAsRead(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        notificationService.markAsRead(authentication.name, id)
        return ResponseEntity.ok().build()
    }

    /**
     * Отметить все уведомления как прочитанные
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    fun markAllAsRead(authentication: Authentication): ResponseEntity<Void> {
        notificationService.markAllAsRead(authentication.name)
        return ResponseEntity.ok().build()
    }

    /**
     * Удалить уведомление
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteNotification(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        notificationService.deleteNotification(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Отправить ручное уведомление (только для ADMIN/MODERATOR)
     * POST /api/notifications/send
     */
    @PostMapping("/send")
    fun sendCustomNotification(
        authentication: Authentication,
        @RequestBody request: CustomNotificationRequest
    ): ResponseEntity<Map<String, Any>> {
        val sentCount = notificationService.sendCustomNotification(authentication.name, request)
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "sentCount" to sentCount,
            "message" to "Notification sent to $sentCount users"
        ))
    }
}

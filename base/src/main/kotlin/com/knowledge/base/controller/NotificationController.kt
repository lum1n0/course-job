package com.knowledge.base.controller

import com.knowledge.base.dto.CustomNotificationRequest
import com.knowledge.base.dto.NotificationDto
import com.knowledge.base.dto.NotificationStatsDto
import com.knowledge.base.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Управление уведомлениями пользователя")
class NotificationController(
    private val notificationService: NotificationService
) {

    @Operation(
        summary = "Получить все уведомления",
        description = "Возвращает все уведомления текущего пользователя с пагинацией",
        parameters = [
            Parameter(name = "page", description = "Номер страницы", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "size", description = "Размер страницы (по умолчанию 20)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список уведомлений успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            )
        ]
    )
    @GetMapping
    fun getUserNotifications(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(hidden = true) @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<NotificationDto>> {
        val notifications = notificationService.getUserNotifications(authentication.name, pageable)
        return ResponseEntity.ok(notifications)
    }

    @Operation(
        summary = "Получить непрочитанные уведомления",
        description = "Возвращает список всех непрочитанных уведомлений текущего пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список непрочитанных уведомлений успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/unread")
    fun getUnreadNotifications(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<NotificationDto>> {
        val notifications = notificationService.getUnreadNotifications(authentication.name)
        return ResponseEntity.ok(notifications)
    }

    @Operation(
        summary = "Получить статистику уведомлений",
        description = "Возвращает статистику уведомлений текущего пользователя (общее количество, непрочитанные и т.д.)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статистика успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = NotificationStatsDto::class)
                )]
            )
        ]
    )
    @GetMapping("/stats")
    fun getNotificationStats(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<NotificationStatsDto> {
        val stats = notificationService.getNotificationStats(authentication.name)
        return ResponseEntity.ok(stats)
    }

    @Operation(
        summary = "Отметить уведомление как прочитанное",
        description = "Помечает указанное уведомление как прочитанное",
        parameters = [
            Parameter(name = "id", description = "ID уведомления", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "200", description = "Уведомление успешно отмечено как прочитанное"),
            ApiResponse(responseCode = "404", description = "Уведомление не найдено")
        ]
    )
    @PutMapping("/{id}/read")
    fun markAsRead(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID уведомления", required = true) @PathVariable id: Long
    ): ResponseEntity<Void> {
        notificationService.markAsRead(authentication.name, id)
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "Отметить все уведомления как прочитанные",
        description = "Помечает все уведомления текущего пользователя как прочитанные",
        responses = [
            ApiResponse(responseCode = "200", description = "Все уведомления успешно отмечены как прочитанные")
        ]
    )
    @PutMapping("/read-all")
    fun markAllAsRead(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<Void> {
        notificationService.markAllAsRead(authentication.name)
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "Удалить уведомление",
        description = "Удаляет указанное уведомление",
        parameters = [
            Parameter(name = "id", description = "ID уведомления", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Уведомление успешно удалено"),
            ApiResponse(responseCode = "404", description = "Уведомление не найдено")
        ]
    )
    @DeleteMapping("/{id}")
    fun deleteNotification(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID уведомления", required = true) @PathVariable id: Long
    ): ResponseEntity<Void> {
        notificationService.deleteNotification(authentication.name, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Отправить пользовательское уведомление",
        description = "Отправляет пользовательское уведомление указанным пользователям (только для ADMIN и MODERATOR)",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные уведомления",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CustomNotificationRequest::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Уведомление успешно отправлено",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PostMapping("/send")
    fun sendCustomNotification(
        @Parameter(hidden = true) authentication: Authentication,
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

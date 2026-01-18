package com.knowledge.base.controller

import com.knowledge.base.dto.FeedbackDto
import com.knowledge.base.mapper.FeedbackMapper
import com.knowledge.base.service.FeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "Управление обратной связью")
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val feedbackMapper: FeedbackMapper,
) {

    @Operation(
        summary = "Создать обращение",
        description = "Создает новое обращение обратной связи от текущего пользователя",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные обращения",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = FeedbackDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Обращение успешно создано",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FeedbackDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные")
        ]
    )
    @PostMapping
    fun createFeedback(
        @RequestBody feedbackDto: FeedbackDto,
        @Parameter(hidden = true) authentication: Authentication
    ): ResponseEntity<FeedbackDto> {
        val userEmail = authentication.name ?: throw IllegalStateException("Не удалось определить пользователя из токена")
        val createFeedbackEntity = feedbackService.addFeedbackRequest(feedbackDto, userEmail)
        val responseDto = feedbackMapper.toDto(createFeedbackEntity)
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto)
    }

    @Operation(
        summary = "Получить все обращения",
        description = "Возвращает все обращения обратной связи с пагинацией",
        parameters = [
            Parameter(name = "page", description = "Номер страницы", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "size", description = "Размер страницы", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список обращений успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            )
        ]
    )
    @GetMapping("/all")
    fun getAllFeedback(
        @Parameter(description = "Номер страницы", required = false) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы", required = false) @RequestParam(defaultValue = "10") size: Int,
        @Parameter(hidden = true) authentication: Authentication
    ): ResponseEntity<Page<FeedbackDto>> {
        val pageable = PageRequest.of(page, size)
        val feedbackPage = feedbackService.getAllFeedback(pageable)
        return ResponseEntity.ok(feedbackPage)
    }

    @Operation(
        summary = "Обновить статус обращения",
        description = "Обновляет статус ответа на обращение",
        parameters = [
            Parameter(name = "id", description = "ID обращения", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "isAnswered", description = "Статус ответа", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статус успешно обновлен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FeedbackDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Обращение не найдено")
        ]
    )
    @PutMapping("/{id}/answer")
    fun updateFeedbackStatus(
        @Parameter(description = "ID обращения", required = true) @PathVariable id: Long,
        @Parameter(description = "Статус ответа", required = true) @RequestParam isAnswered: Boolean,
        @Parameter(hidden = true) authentication: Authentication
    ): ResponseEntity<FeedbackDto> {
        val updatedFeedback = feedbackService.updateFeedbackStatus(id, isAnswered)
        val responseDto = feedbackMapper.toDto(updatedFeedback)
        return ResponseEntity.ok(responseDto)
    }
}

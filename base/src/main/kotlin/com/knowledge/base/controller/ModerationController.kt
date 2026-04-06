package com.knowledge.base.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.service.ModerationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "Модерация статей и заявок")
class ModerationController(
    private val moderationService: ModerationService,
    private val objectMapper: ObjectMapper
) {

    @Operation(
        summary = "Отправить заявку на создание статьи",
        description = "Создает заявку на создание новой статьи (для WRITER)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Заявка успешно создана",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные")
        ]
    )
    @PostMapping("/submit/create", consumes = ["multipart/form-data"])
    fun submitCreate(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "Название статьи", required = true) @RequestParam("title") title: String,
        @Parameter(description = "Описание статьи в формате JSON (Quill Delta)", required = true) @RequestParam("description") descriptionJson: String,
        @Parameter(description = "ID категории", required = true) @RequestParam("categoryId") categoryId: Long,
        @Parameter(description = "Видео файл (mp4, avi, mov, wmv, max 100MB)", required = false) 
        @RequestParam("videoFile", required = false) 
        @Schema(type = "string", format = "binary") 
        videoFile: MultipartFile?,
        @Parameter(description = "Дополнительные файлы (pdf, doc, xls и др., max 50MB каждый)", required = false) 
        @RequestParam("files", required = false) 
        @ArraySchema(schema = Schema(type = "string", format = "binary"))
        files: List<MultipartFile>?
    ): ResponseEntity<ArticleProposalDto> {
        val desc = objectMapper.readTree(descriptionJson)
        val dto = moderationService.submitCreate(authentication.name, title, desc, categoryId, videoFile, files)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Отправить заявку на обновление статьи",
        description = "Создает заявку на обновление существующей статьи (для WRITER)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Заявка успешно создана",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные"),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @PostMapping("/submit/update/{articleId}", consumes = ["multipart/form-data"])
    fun submitUpdate(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Новое название статьи", required = true) @RequestParam("title") title: String,
        @Parameter(description = "Новое описание статьи в формате JSON (Quill Delta)", required = true) @RequestParam("description") descriptionJson: String,
        @Parameter(description = "Новый ID категории", required = true) @RequestParam("categoryId") categoryId: Long,
        @Parameter(description = "Видео файл (mp4, avi, mov, wmv, max 100MB)", required = false) 
        @RequestParam("videoFile", required = false) 
        @Schema(type = "string", format = "binary") 
        videoFile: MultipartFile?,
        @Parameter(description = "Дополнительные файлы (pdf, doc, xls и др., max 50MB каждый)", required = false) 
        @RequestParam("files", required = false) 
        @ArraySchema(schema = Schema(type = "string", format = "binary"))
        files: List<MultipartFile>?
    ): ResponseEntity<ArticleProposalDto> {
        val desc = objectMapper.readTree(descriptionJson)
        val dto = moderationService.submitUpdate(authentication.name, articleId, title, desc, categoryId, videoFile, files)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Получить заявки на модерацию",
        description = "Возвращает список заявок, ожидающих модерации (для MODERATOR и ADMIN)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список заявок успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/pending")
    fun listPending(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<ArticleProposalDto>> {
        return ResponseEntity.ok(moderationService.listPending(authentication.name))
    }

    @Operation(
        summary = "Одобрить заявку",
        description = "Одобряет заявку на создание/обновление статьи (для MODERATOR и ADMIN)",
        parameters = [
            Parameter(name = "id", description = "ID заявки", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "comment", description = "Комментарий модератора", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Заявка успешно одобрена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Заявка не найдена")
        ]
    )
    @PostMapping("/proposals/{id}/approve")
    fun approve(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID заявки", required = true) @PathVariable id: Long,
        @Parameter(description = "Комментарий модератора", required = false) @RequestParam(required = false) comment: String?
    ): ResponseEntity<ArticleProposalDto> {
        val dto = moderationService.approve(id, authentication.name, comment)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Отклонить заявку",
        description = "Отклоняет заявку на создание/обновление статьи (для MODERATOR и ADMIN)",
        parameters = [
            Parameter(name = "id", description = "ID заявки", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "reason", description = "Причина отклонения", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Заявка успешно отклонена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Заявка не найдена")
        ]
    )
    @PostMapping("/proposals/{id}/reject")
    fun reject(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID заявки", required = true) @PathVariable id: Long,
        @Parameter(description = "Причина отклонения", required = true) @RequestParam reason: String
    ): ResponseEntity<ArticleProposalDto> {
        val dto = moderationService.reject(id, authentication.name, reason)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Получить заявку по ID",
        description = "Возвращает детальную информацию о заявке",
        parameters = [
            Parameter(name = "id", description = "ID заявки", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Заявка успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Заявка не найдена")
        ]
    )
    @GetMapping("/proposals/{id}")
    fun getProposal(
        @Parameter(description = "ID заявки", required = true) @PathVariable id: Long,
        @Parameter(hidden = true) authentication: Authentication
    ): ResponseEntity<ArticleProposalDto> {
        return ResponseEntity.ok(moderationService.getProposal(id, authentication.name))
    }
    @Operation(
        summary = "Получить историю проверенных заявок",
        description = "Возвращает список заявок, которые были одобрены или отклонены текущим модератором",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "История успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/completed")
    fun listCompleted(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<ArticleProposalDto>> {
        return ResponseEntity.ok(moderationService.listCompleted(authentication.name))
    }

}

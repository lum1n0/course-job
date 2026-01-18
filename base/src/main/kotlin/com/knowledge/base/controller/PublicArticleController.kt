package com.knowledge.base.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.service.ModerationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/public/articles")
@Tag(name = "Public Articles", description = "Публичная отправка статей")
class PublicArticleController(
    private val moderationService: ModerationService,
    private val objectMapper: ObjectMapper
) {

    @Operation(
        summary = "Отправить публичную статью",
        description = "Позволяет авторизованному пользователю отправить статью на модерацию",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для создания публичной статьи",
            required = true,
            content = [Content(
                mediaType = "multipart/form-data"
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно отправлена на модерацию",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleProposalDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные")
        ]
    )
    @PostMapping("/submit", consumes = ["multipart/form-data"])
    fun submitPublicArticle(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "Название статьи", required = true) @RequestParam("title") title: String,
        @Parameter(description = "Описание статьи в формате JSON", required = true) @RequestParam("description") descriptionJson: String,
        @Parameter(description = "ID категории", required = true) @RequestParam("categoryId") categoryId: Long,
        @Parameter(description = "Видео файл", required = false) @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @Parameter(description = "Дополнительные файлы", required = false) @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleProposalDto> {
        val descriptionNode = objectMapper.readTree(descriptionJson)
        val dto = moderationService.submitPublicCreate(
            authentication.name,
            title,
            descriptionNode,
            categoryId,
            videoFile,
            files
        )
        return ResponseEntity.ok(dto)
    }
}

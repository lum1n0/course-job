package com.knowledge.base.controller

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/my")
@Tag(name = "My Work", description = "Управление личными работами пользователя")
class MyWorkController(
    private val moderationService: ModerationService
) {

    @Operation(
        summary = "Получить мои работы",
        description = "Возвращает список работ текущего пользователя (созданные и отредактированные статьи со статусами и комментариями модераторов)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список работ успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/work")
    fun myWork(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<ArticleProposalDto>> {
        val list = moderationService.listMyWork(authentication.name)
        return ResponseEntity.ok(list)
    }
}

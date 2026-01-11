// src/main/kotlin/com/knowledge/base/controller/ModerationController.kt
package com.knowledge.base.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.service.ModerationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/moderation")
class ModerationController(
    private val moderationService: ModerationService,
    private val objectMapper: ObjectMapper
) {
    // WRITER -> заявка на создание
    @PostMapping("/submit/create", consumes = ["multipart/form-data"])
    fun submitCreate(
        authentication: Authentication,
        @RequestParam("title") title: String,
        @RequestParam("description") descriptionJson: String,
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleProposalDto> {
        val desc = objectMapper.readTree(descriptionJson)
        val dto = moderationService.submitCreate(authentication.name, title, desc, categoryId, videoFile, files)
        return ResponseEntity.ok(dto)
    }

    // WRITER -> заявка на обновление
    @PostMapping("/submit/update/{articleId}", consumes = ["multipart/form-data"])
    fun submitUpdate(
        authentication: Authentication,
        @PathVariable articleId: Long,
        @RequestParam("title") title: String,
        @RequestParam("description") descriptionJson: String,
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleProposalDto> {
        val desc = objectMapper.readTree(descriptionJson)
        val dto = moderationService.submitUpdate(authentication.name, articleId, title, desc, categoryId, videoFile, files)
        return ResponseEntity.ok(dto)
    }

    // MODERATOR/ADMIN -> список на модерации
    @GetMapping("/pending")
    fun listPending(authentication: Authentication): ResponseEntity<List<ArticleProposalDto>> {
        return ResponseEntity.ok(moderationService.listPending(authentication.name))
    }
    // MODERATOR/ADMIN -> одобрение
    @PostMapping("/proposals/{id}/approve")
    fun approve(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestParam(required = false) comment: String?
    ): ResponseEntity<ArticleProposalDto> {
        val dto = moderationService.approve(id, authentication.name, comment)
        return ResponseEntity.ok(dto)
    }

    // MODERATOR/ADMIN -> отклонение
    @PostMapping("/proposals/{id}/reject")
    fun reject(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestParam reason: String
    ): ResponseEntity<ArticleProposalDto> {
        val dto = moderationService.reject(id, authentication.name, reason)
        return ResponseEntity.ok(dto)
    }

    // Просмотр заявки
    @GetMapping("/proposals/{id}")
    fun getProposal(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<ArticleProposalDto> {
        return ResponseEntity.ok(moderationService.getProposal(id, authentication.name))
    }
}

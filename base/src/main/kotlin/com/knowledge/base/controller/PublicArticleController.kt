package com.knowledge.base.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.service.ModerationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/public/articles")
class PublicArticleController(
    private val moderationService: ModerationService,
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/submit", consumes = ["multipart/form-data"])
    fun submitPublicArticle(
        authentication: Authentication,
        @RequestParam("title") title: String,
        @RequestParam("description") descriptionJson: String,
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @RequestParam("files", required = false) files: List<MultipartFile>?
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

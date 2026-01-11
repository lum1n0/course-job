// src/main/kotlin/com/knowledge/base/controller/MyWorkController.kt
package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.service.ModerationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/my")
class MyWorkController(
    private val moderationService: ModerationService
) {
    // Список «мои работы»: созданные, отредактированные + статусы и комментарии
    @GetMapping("/work")
    fun myWork(authentication: Authentication): ResponseEntity<List<ArticleProposalDto>> {
        val list = moderationService.listMyWork(authentication.name)
        return ResponseEntity.ok(list)
    }
}

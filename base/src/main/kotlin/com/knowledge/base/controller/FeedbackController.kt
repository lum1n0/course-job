package com.knowledge.base.controller

import com.knowledge.base.dto.FeedbackDto
import com.knowledge.base.mapper.FeedbackMapper
import com.knowledge.base.service.FeedbackService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val feedbackMapper: FeedbackMapper,
) {

    @PostMapping
    fun createFeedback(
        @RequestBody feedbackDto: FeedbackDto,
        authentication: Authentication
    ): ResponseEntity<FeedbackDto> {
        val userEmail = authentication.name ?: throw IllegalStateException("Не удалось определить пользователя из токена")
        val createFeedbackEntity = feedbackService.addFeedbackRequest(feedbackDto, userEmail)
        val responseDto = feedbackMapper.toDto(createFeedbackEntity)
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto)
    }

    @GetMapping("/all")
    fun getAllFeedback(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication
    ): ResponseEntity<Page<FeedbackDto>> {
        val pageable = PageRequest.of(page, size)
        val feedbackPage = feedbackService.getAllFeedback(pageable)
        return ResponseEntity.ok(feedbackPage)
    }

    @PutMapping("/{id}/answer")
    fun updateFeedbackStatus(
        @PathVariable id: Long,
        @RequestParam isAnswered: Boolean,
        authentication: Authentication
    ): ResponseEntity<FeedbackDto> {
        val updatedFeedback = feedbackService.updateFeedbackStatus(id, isAnswered)
        val responseDto = feedbackMapper.toDto(updatedFeedback)
        return ResponseEntity.ok(responseDto)
    }
}

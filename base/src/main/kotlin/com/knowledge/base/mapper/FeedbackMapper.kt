package com.knowledge.base.mapper

import com.knowledge.base.dto.FeedbackDto
import com.knowledge.base.model.Feedback
import org.modelmapper.ModelMapper
import org.springframework.stereotype.Component

@Component
class FeedbackMapper(private val modelMapper: ModelMapper) {
    fun toDto(feedback: Feedback): FeedbackDto {
        return FeedbackDto(
            id = feedback.id,
            title = feedback.title,
            description = feedback.description,
            userEmail = feedback.user.email, // Извлекаем почту пользователя
            articleTitle = feedback.article.title, // Извлекаем заголовок статьи
            isAnswered = feedback.isAnswered
        )
    }

    fun toEntity(dto: FeedbackDto): Feedback {
        throw NotImplementedError("Mapping from FeedbackDto to Feedback is not implemented")
    }
}
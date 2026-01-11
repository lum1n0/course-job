package com.knowledge.base.dto

import com.fasterxml.jackson.databind.JsonNode

data class TextDeltaDto(
    val type: String, // "INSERT" | "DELETE" | "CHANGE"
    val sourcePosition: Int,
    val sourceSize: Int,
    val targetPosition: Int,
    val targetSize: Int,
    val source: String,
    val target: String
)

data class FilesDiffDto(
    val added: List<String>,
    val removed: List<String>,
    val unchanged: List<String>
)

data class CompareResultDto(
    val fromVersion: Int,
    val toVersion: Int,

    // Заголовок
    val titleChanged: Boolean,
    val titleBefore: String,
    val titleAfter: String,

    // Категория
    val categoryChanged: Boolean,
    val categoryBeforeId: Long?,
    val categoryBeforeName: String?, // Человекочитаемое имя/описание
    val categoryAfterId: Long,
    val categoryAfterName: String,

    // Контент и вложения
    val descriptionJsonPatch: JsonNode,            // JSON Patch (RFC 6902)
    val descriptionTextDeltas: List<TextDeltaDto>, // Текстовые дельты по токенам
    val filesDiff: FilesDiffDto,
    val videosDiff: FilesDiffDto
)

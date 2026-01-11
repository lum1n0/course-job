// src/main/kotlin/com/knowledge/base/util/QuillParserUtil.kt
package com.knowledge.base.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class QuillParserUtil {

    private val mapper = ObjectMapper()

    // Универсальная очистка: принимает любой формат и возвращает чистый текст
    fun sanitizePreviewAnyFormat(input: Any?, maxLen: Int = 300): String {
        val text = extractText(input)
        val clean = normalizeWhitespace(text)
        return if (clean.length > maxLen) clean.substring(0, maxLen) + "..." else clean
    }

    // Извлекает текст из разных форматов: Quill Delta (JsonNode или String), HTML, Plain
    fun extractText(description: Any?): String {
        if (description == null) return ""
        return when (description) {
            is JsonNode -> extractFromJsonNode(description)
            is String -> extractFromString(description)
            else -> extractFromString(description.toString())
        }
    }

    private fun extractFromJsonNode(node: JsonNode): String {
        // Если это Quill delta вида {"ops":[...]} — собрать insert'ы
        val ops = node.get("ops")
        return if (ops != null && ops.isArray) {
            val sb = StringBuilder()
            for (op in ops) {
                val insertNode = op.get("insert")
                if (insertNode != null && !insertNode.isNull) {
                    val insert = if (insertNode.isTextual) insertNode.asText() else insertNode.toString()
                    sb.append(insert)
                }
            }
            stripMarkup(sb.toString())
        } else {
            // Иначе — просто как строку и снять разметку
            stripMarkup(node.toString())
        }
    }

    private fun extractFromString(raw: String): String {
        // Пытаемся понять, это JSON Quill или нет
        val trimmed = raw.trim()
        val asText = if (looksLikeJson(trimmed)) {
            try {
                val json = mapper.readTree(trimmed)
                extractFromJsonNode(json)
            } catch (_: Exception) {
                stripMarkup(trimmed)
            }
        } else {
            stripMarkup(trimmed)
        }
        return asText
    }

    private fun looksLikeJson(s: String): Boolean {
        if (s.isEmpty()) return false
        val c0 = s.first()
        val cN = s.last()
        return (c0 == '{' && cN == '}') || (c0 == '[' && cN == ']')
    }

    // Снятие HTML/Quill-артефактов
    private fun stripMarkup(s: String): String {
        return s
            .replace(Regex("<[^>]*>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("&nbsp;|&lt;|&gt;|&amp;", RegexOption.IGNORE_CASE), " ")
    }

    fun normalizeWhitespace(s: String): String {
        return s.replace(Regex("\\s+"), " ").trim()
    }

    // Пригодно, если нужно порезать на чанки для индексации
    fun chunk(text: String, chunkSize: Int = 900, overlap: Int = 150): List<String> {
        val clean = normalizeWhitespace(extractText(text))
        if (clean.isBlank()) return emptyList()
        val chunks = mutableListOf<String>()
        var start = 0
        val step = (chunkSize - overlap).coerceAtLeast(1)
        while (start < clean.length) {
            var end = (start + chunkSize).coerceAtMost(clean.length)
            var slice = clean.substring(start, end)
            
            if (end < clean.length) {
                val lastPunct = slice.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
                if (lastPunct > 200) slice = slice.substring(0, lastPunct + 1)
            }
            chunks.add(slice.trim())
            if (end == clean.length) break
            start += step
        }
        return chunks
    }
}
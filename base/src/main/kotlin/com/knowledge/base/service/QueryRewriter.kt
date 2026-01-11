package com.knowledge.base.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

data class RewriteResult(
    val rewritten: String,
    val keyphrases: Set<String>
)

@Service
class QueryRewriter(
    private val restTemplate: RestTemplate,
    @Value("\${spring.ai.ollama.base-url}") private val ollamaBaseUrl: String,
    @Value("\${spring.ai.ollama.rewrite.model:qwen2.5:1.5b-instruct}") private val rewriteModel: String
) {
    private val logger = LoggerFactory.getLogger(QueryRewriter::class.java)

    fun rewrite(query: String): RewriteResult {
        val base = query.trim()
        if (base.length >= 80) {
            return RewriteResult(base, extractTerms(base))
        }
        return try {
            val url = "$ollamaBaseUrl/api/generate"
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val prompt = """
                Переформулируй запрос кратко и по делу для поиска в базе знаний. Выдели ключевые фразы (3–6), через запятую.
                Формат ответа строго:
                QUERY: <одна краткая строка>
                KEYS: <список через запятую>
                Запрос: "$base"
            """.trimIndent()
            // Формируем корректный JSON вручную: ключи и значения экранируются, поле prompt идёт строкой
            val bodyJson = """{"model":${json(rewriteModel)},"prompt":${json(prompt)},"stream":false}"""
            val entity = HttpEntity(bodyJson, headers)
            val raw = restTemplate.postForObject(url, entity, String::class.java).orEmpty()
            val responseField = extractJsonField(raw, "response")?.trim().orEmpty()
            val source = if (responseField.isNotBlank()) responseField else raw

            val q = Regex("(?im)^\\s*QUERY:\\s*(.+)$").find(source)?.groupValues?.get(1)?.trim()
            val keysLine = Regex("(?im)^\\s*KEYS:\\s*(.+)$").find(source)?.groupValues?.get(1)?.trim()
            val keys = keysLine
                ?.split(',')
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()

            val rewritten = if (!q.isNullOrBlank()) q else base
            val mergedKeys = ((keys + extractTerms(base)).take(8)).toSet()
            RewriteResult(rewritten, mergedKeys)
        } catch (e: Exception) {
            logger.warn("Query rewrite fallback: ${e.message}")
            RewriteResult(base, extractTerms(base))
        }
    }

    private fun extractTerms(text: String): Set<String> {
        val splitter = Regex("""\s+|[,.;:!?()\${'$'}\[\]{}"'`]+""")
        return text
            .lowercase()
            .split(splitter)
            .map { it.trim() }
            .filter { it.length >= 3 }
            .toSet()
    }

    // Минимальный JSON-эскейп для строки
    private fun json(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
        return "\"$escaped\""
    }

    // Быстрый парсер поля верхнего уровня из JSON-ответа Ollama
    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex("(?s)\"${Regex.escape(field)}\"\\s*:\\s*\"(.*?)\"")
        val m = pattern.find(json) ?: return null
        return m.groupValues.getOrNull(1)
    }
}

// src/main/kotlin/com/knowledge/base/service/IndexingService.kt
package com.knowledge.base.service

import com.knowledge.base.model.Article
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.util.QuillParserUtil
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Service
class IndexingService(
    private val articleRepository: ArticleRepository,
    private val vectorStore: VectorStore,
    private val quillParser: QuillParserUtil,
    private val restTemplate: RestTemplate,
    private val luceneService: LuceneService,
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${spring.ai.ollama.base-url}") private val ollamaBaseUrl: String,
    @Value("\${spring.ai.ollama.embedding.model}") private val embeddingModelName: String
) {
    private val logger = LoggerFactory.getLogger(IndexingService::class.java)

    @EventListener(ApplicationReadyEvent::class)
    @Async
    fun initialIndexingAfterStartup() {
        logger.info("Ожидание модели эмбеддингов '$embeddingModelName' в Ollama...")
        if (waitForEmbeddingModel()) {
            logger.info("Модель '$embeddingModelName' доступна. Полная индексация будет выполняться по расписанию (пятница 23:00).")
        } else {
            logger.error("Модель '$embeddingModelName' не загрузилась вовремя. Индексация будет по расписанию.")
        }
    }

    private fun waitForEmbeddingModel(): Boolean {
        val maxRetries = 18
        val modelsApiUrl = "$ollamaBaseUrl/api/tags"
        repeat(maxRetries) {
            try {
                val response = restTemplate.getForObject(modelsApiUrl, String::class.java)
                if (response?.contains(embeddingModelName) == true) return true
            } catch (_: Exception) {
                // ignore and retry
            }
            Thread.sleep(5_000)
        }
        return false
    }

    // Пятница 23:00 по Москве
    @Scheduled(cron = "0 0 23 ? * FRI", zone = "Europe/Moscow")
    @Async
    @Transactional
    fun performIndexing() {
        try {
            logger.info("Начало полной индексации статей...")
            val articles: List<Article> = articleRepository.findAllWithCategoryAndAccessRoles()
            if (articles.isEmpty()) {
                logger.info("Статьи для индексации не найдены.")
                return
            }

            val documents = mutableListOf<Document>()
            val luceneDocs = mutableListOf<Map<String, Any>>()

            for (article in articles) {
                val textContent = quillParser.extractText(article.description)
                if (textContent.isBlank()) continue

                val keyphrases = autoKeyphrases(article.title ?: "", textContent)
                val categoryId = article.category?.id ?: 0L
                val categoryTitle = article.category?.description ?: ""
                val accessRoleIds = article.category?.accessRoles?.mapNotNull { it.id?.toString() } ?: emptyList()
                val accessRoleTitles = article.category?.accessRoles?.mapNotNull { it.title } ?: emptyList()

                // Чанки для VectorStore
                val chunks = quillParser.chunk(textContent, chunkSize = 900, overlap = 150)
                chunks.forEachIndexed { idx, chunk ->
                    val documentId = UUID.nameUUIDFromBytes("${article.id}#$idx".toByteArray()).toString()
                    documents += Document(
                        documentId,
                        chunk,
                        mapOf(
                            "articleId" to (article.id ?: 0L),
                            "title" to (article.title ?: ""),
                            "categoryId" to categoryId,
                            "categoryTitle" to categoryTitle,
                            "accessRoles" to accessRoleIds,
                            "accessRoleTitles" to accessRoleTitles,
                            "keywords" to keyphrases
                        )
                    )
                }

                // Целый документ для BM25 Lucene
                luceneDocs += mapOf(
                    "articleId" to (article.id ?: 0L),
                    "title" to (article.title ?: ""),
                    "body" to textContent
                )
            }

            if (documents.isNotEmpty()) {
                val startAll = System.currentTimeMillis()
                var done = 0
                val batchSize = 100
                documents.chunked(batchSize).forEachIndexed { i, batch ->
                    var attempts = 0
                    var ok = false
                    val started = System.currentTimeMillis()
                    while (attempts < 3 && !ok) {
                        try {
                            vectorStore.add(batch)
                            ok = true
                        } catch (e: Exception) {
                            attempts++
                            logger.warn("Batch ${i + 1}/${(documents.size + batchSize - 1) / batchSize} failed on attempt $attempts: ${e.message}")
                            Thread.sleep(1500L * attempts)
                            if (attempts >= 3) throw e
                        }
                    }
                    done += batch.size
                    val took = (System.currentTimeMillis() - started) / 1000.0
                    logger.info("VectorStore progress: $done/${documents.size} (+${batch.size}) за ${"%.2f".format(took)} c")
                }
                val total = (System.currentTimeMillis() - startAll) / 1000.0
                logger.info("VectorStore: ${documents.size} документов добавлено за ${"%.2f".format(total)} c.")
            }

            luceneService.rebuildIndex(luceneDocs)
            logger.info("Lucene: ${luceneDocs.size} документов проиндексировано.")
        } catch (e: Exception) {
            logger.error("Ошибка индексации: ${e.message}", e)
        }
    }

    @Async
    @Transactional
    fun indexArticle(article: Article) {
        try {
            val textContent = quillParser.extractText(article.description)
            if (textContent.isBlank()) return

            val keyphrases = autoKeyphrases(article.title ?: "", textContent)
            val categoryId = article.category?.id ?: 0L
            val categoryTitle = article.category?.description ?: ""
            val accessRoleIds = article.category?.accessRoles?.mapNotNull { it.id?.toString() } ?: emptyList()
            val accessRoleTitles = article.category?.accessRoles?.mapNotNull { it.title } ?: emptyList()

            // Чанки для VectorStore
            val chunks = quillParser.chunk(textContent, chunkSize = 900, overlap = 150)
            val documents = chunks.mapIndexed { idx, chunk ->
                val documentId = UUID.nameUUIDFromBytes("${article.id}#$idx".toByteArray()).toString()
                Document(
                    documentId,
                    chunk,
                    mapOf(
                        "articleId" to (article.id ?: 0L),
                        "title" to (article.title ?: ""),
                        "categoryId" to categoryId,
                        "categoryTitle" to categoryTitle,
                        "accessRoles" to accessRoleIds,
                        "accessRoleTitles" to accessRoleTitles,
                        "keywords" to keyphrases
                    )
                )
            }

            vectorStore.add(documents)

            // Для Lucene
            val luceneDoc = mapOf(
                "articleId" to (article.id ?: 0L),
                "title" to (article.title ?: ""),
                "body" to textContent
            )
            luceneService.addDocument(luceneDoc)

            logger.info("Индексирована статья: ${article.id}")
        } catch (e: Exception) {
            logger.error("Ошибка индексации статьи ${article.id}: ${e.message}", e)
        }
    }

    @Async
    fun updateArticle(article: Article) {
        deleteArticle(article.id ?: return)
        indexArticle(article)
    }

    @Async
    fun deleteArticle(articleId: Long) {
        try {
            // ВАЖНО: удаляем из pgvector без similaritySearch и эмбеддингов — только по метаданным articleId
            jdbcTemplate.update(
                "DELETE FROM vector_store WHERE (metadata->>'articleId')::bigint = ?",
                articleId
            )

            // Для Lucene
            luceneService.deleteDocument(articleId)

            logger.info("Удалена индексация статьи: $articleId")
        } catch (e: Exception) {
            logger.error("Ошибка удаления индексации статьи $articleId: ${e.message}", e)
        }
    }

    private fun autoKeyphrases(title: String, body: String): List<String> {
        val text = (title + " " + body).lowercase()
        val tokens = text.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.length >= 3 }
        if (tokens.isEmpty()) return emptyList()

        val tf = tokens.groupingBy { it }.eachCount()
        val topWords = tf.entries.sortedByDescending { it.value }.take(12).map { it.key }.toMutableSet()

        // N-gram 2-3 слов (из title отдаем приоритет)
        val titleTokens = title.lowercase().split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.length >= 2 }
        val ngrams = mutableSetOf<String>()
        for (n in 2..3) {
            titleTokens.windowed(n, 1, false).forEach { ngrams += it.joinToString(" ") }
        }

        return (topWords + ngrams).take(16).toList()
    }
    @Async
    @Transactional(readOnly = true)
    fun indexArticleById(articleId: Long) {
        // Обязательно тянем category + accessRoles одним запросом
        val article = articleRepository.findByIdWithCategoryAndRoles(articleId) ?: return

        val textContent = quillParser.extractText(article.description)
        if (textContent.isBlank()) return

        val categoryId = article.category?.id ?: 0L
        val categoryTitle = article.category?.description ?: ""
        val accessRoleIds = article.category?.accessRoles?.mapNotNull { it.id?.toString() } ?: emptyList()
        val accessRoleTitles = article.category?.accessRoles?.mapNotNull { it.title } ?: emptyList()

        val keyphrases = autoKeyphrases(article.title ?: "", textContent)
        val chunks = quillParser.chunk(textContent, chunkSize = 900, overlap = 150)
        val documents = chunks.mapIndexed { idx, chunk ->
            val documentId = UUID.nameUUIDFromBytes("${article.id}#$idx".toByteArray()).toString()
            Document(
                documentId,
                chunk,
                mapOf(
                    "articleId" to (article.id ?: 0L),
                    "title" to (article.title ?: ""),
                    "categoryId" to categoryId,
                    "categoryTitle" to categoryTitle,
                    "accessRoles" to accessRoleIds,
                    "accessRoleTitles" to accessRoleTitles,
                    "keywords" to keyphrases
                )
            )
        }
        vectorStore.add(documents)

        val luceneDoc = mapOf(
            "articleId" to (article.id ?: 0L),
            "title" to (article.title ?: ""),
            "body" to textContent
        )
        luceneService.addDocument(luceneDoc)
        logger.info("Индексирована статья: ${article.id}")
    }
}

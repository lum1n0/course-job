package com.knowledge.base.service

import com.knowledge.base.model.Article
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.service.LuceneService
import com.knowledge.base.util.QuillParserUtil
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Collator
import java.util.Comparator
import java.util.Locale

@Service
class OllamaService(
    private val vectorStore: VectorStore,
    private val userRepository: UserRepository,
    private val articleRepository: ArticleRepository,
    private val luceneService: LuceneService,
    private val queryRewriter: QueryRewriter,
    private val quillParserUtil: QuillParserUtil
) {
    private val logger = LoggerFactory.getLogger(OllamaService::class.java)

    fun getGreetingMessage(): String = "Привет! Я ваш помощник по базе знаний. Чем могу помочь?"

    @Transactional(readOnly = true)
    fun generateResponse(userMessage: String, userId: Long): String {
        val original = userMessage.trim()
        val lower = original.lowercase()
        val cleaned = lower.replace(Regex("[^а-яёa-z0-9 ]"), "").trim() // Удаляем пунктуацию и специальные символы, оставляем буквы, цифры и пробелы

        when (cleaned) {
            "привет", "здравствуй", "здравствуйте", "добрый день", "добрый вечер" -> return "Здравствуйте! Чем могу помочь?"
            "спасибо", "благодарю" -> return "Пожалуйста! Рад был помочь."
            "пока", "до свидания", "всего доброго" -> return "До свидания! Обращайтесь, если снова понадобится помощь."
        }

        val user = userRepository.findByIdWithAccessRoles(userId).orElse(null)
            ?: return "Ошибка: пользователь не найден."

        val hasFullAccess = user.accessRoles.any { it.title == "FULL_ACCESS" }
        val allowedCategoryIds: Set<Long> = if (hasFullAccess) emptySet()
        else user.accessRoles.flatMap { it.categories }.mapNotNull { it.id }.toSet()

        // Query rewrite + keyphrases
        val rewrite = queryRewriter.rewrite(original)
        val effectiveQuery = rewrite.rewritten
        val queryTerms = rewrite.keyphrases

        // Прямой поиск по названию
        val directAll = articleRepository.findByTitleContainingIgnoreCaseAndIsDeleteFalse(original)
        val directFiltered = (if (hasFullAccess) directAll else directAll.filter { it.category?.id in allowedCategoryIds })
            .distinctBy { it.id }

        if (directFiltered.isNotEmpty()) {
            val prioritized = directFiltered.sortedWith(
                Comparator { a, b ->
                    fun score(art: Article): Int {
                        val t = (art.title ?: "").lowercase()
                        var s = 0
                        queryTerms.forEach { if (t.contains(it)) s += 1 }
                        if (t.contains("инструкция") || t.contains("как")) s += 1
                        return s
                    }
                    val diff = score(b) - score(a)
                    if (diff != 0) return@Comparator diff
                    val collator = Collator.getInstance(Locale("ru", "RU"))
                    return@Comparator collator.compare(a.title ?: "", b.title ?: "")
                }
            )
            return formatDirectMatches(prioritized)
        }

        // Гибридный поиск
        val luceneHits = luceneService.search(effectiveQuery, topK = 10)
        val luceneIds = luceneHits.map { it.articleId }.toSet()

        val topK = if (hasFullAccess) 12 else 50
        val searchRequest = SearchRequest.query(effectiveQuery).withTopK(topK)

        var vecCandidates = vectorStore.similaritySearch(searchRequest)
            .filter { (it.metadata["articleId"] as? Number)?.toLong() != null }
            .distinctBy { (it.metadata["articleId"] as Number).toLong() }

        if (!hasFullAccess) {
            val userRoleIds = user.accessRoles.mapNotNull { it.id?.toString() }.toSet()
            if (userRoleIds.isEmpty()) return "У вас нет доступа к статьям. Обратитесь к администратору."
            vecCandidates = vecCandidates.filter { doc ->
                val docRoles = (doc.metadata["accessRoles"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                docRoles.intersect(userRoleIds).isNotEmpty()
            }
        }

        // Объединение
        val merged = mutableListOf<Document>()
        val seen = mutableSetOf<Long>()
        for (h in luceneHits) {
            val doc = vecCandidates.firstOrNull { ((it.metadata["articleId"] as Number).toLong() == h.articleId) }
            if (doc != null && seen.add(h.articleId)) merged += doc
        }
        for (d in vecCandidates) {
            val id = (d.metadata["articleId"] as Number).toLong()
            if (seen.add(id)) merged += d
        }

        if (merged.isEmpty()) {
            return "К сожалению, я не нашёл подходящих статей по вашему запросу в доступной вам базе знаний. Пожалуйста, уточните запрос."
        }

        // Реранк
        val reranked = merged.sortedWith(
            Comparator { a, b ->
                fun score(doc: Document): Int {
                    val title = (doc.metadata["title"] as? String ?: "").lowercase()
                    val kws = ((doc.metadata["keywords"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList())
                        .map { it.lowercase() }.toSet()
                    val contentSnippet = doc.content.lowercase().take(700)
                    var s = 0
                    queryTerms.forEach {
                        if (title.contains(it)) s += 2
                        if (contentSnippet.contains(it)) s += 1
                        if (it in kws) s += 1
                    }
                    val id = (doc.metadata["articleId"] as Number).toLong()
                    if (luceneIds.contains(id)) s += 1
                    if (title.contains("инструкция") || "инструкция" in kws || "setup" in kws) s += 1
                    return s
                }
                val diff = score(b) - score(a)
                if (diff != 0) return@Comparator diff
                val collator = Collator.getInstance(Locale("ru", "RU"))
                val at = (a.metadata["title"] as? String) ?: ""
                val bt = (b.metadata["title"] as? String) ?: ""
                return@Comparator collator.compare(at, bt)
            }
        )

        // Проверка на существование статей (фильтр удаленных)
        val articleIds = reranked.map { (it.metadata["articleId"] as Number).toLong() }
        val existingArticleIds = articleRepository.findAllById(articleIds)
            .filter { !it.isDelete }
            .map { it.id }
            .toSet()

        val validReranked = reranked.filter { (it.metadata["articleId"] as Number).toLong() in existingArticleIds }

        if (validReranked.isEmpty()) {
            return "К сожалению, я не нашёл подходящих статей по вашему запросу в доступной вам базе знаний. Пожалуйста, уточните запрос."
        }

        // Проверка на релевантность (если максимальный score < 2, просить уточнить)
        fun computeScore(doc: Document): Int {
            val title = (doc.metadata["title"] as? String ?: "").lowercase()
            val kws = ((doc.metadata["keywords"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList())
                .map { it.lowercase() }.toSet()
            val contentSnippet = doc.content.lowercase().take(700)
            var s = 0
            queryTerms.forEach {
                if (title.contains(it)) s += 2
                if (contentSnippet.contains(it)) s += 1
                if (it in kws) s += 1
            }
            val id = (doc.metadata["articleId"] as Number).toLong()
            if (luceneIds.contains(id)) s += 1
            if (title.contains("инструкция") || "инструкция" in kws || "setup" in kws) s += 1
            return s
        }

        val maxScore = validReranked.maxOfOrNull { computeScore(it) } ?: 0
        if (maxScore < 2) {
            return "Ваш запрос кажется слишком общим или нерелевантным. Пожалуйста, уточните, что именно вы ищете, чтобы я мог лучше помочь."
        }

        if (isTopHitConfident(validReranked, queryTerms)) {
            return formatSingleArticleResponse(validReranked.first())
        }

        return if (validReranked.size == 1) formatSingleArticleResponse(validReranked.first())
        else formatMultipleArticlesResponse(validReranked.take(6))
    }

    private fun isTopHitConfident(docs: List<Document>, terms: Set<String>): Boolean {
        if (docs.size < 2) return false

        fun score(doc: Document): Int {
            val title = (doc.metadata["title"] as? String ?: "").lowercase()
            val kws = ((doc.metadata["keywords"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList())
                .map { it.lowercase() }.toSet()
            val content = doc.content.lowercase().take(700)
            var sum = 0
            terms.forEach {
                if (title.contains(it)) sum += 2
                if (content.contains(it)) sum += 1
                if (it in kws) sum += 1
            }
            return sum
        }
        val s0 = score(docs[0])
        val s1 = if (docs.size > 1) score(docs[1]) else 0
        return s0 >= s1 + 2
    }

    // Форматирование ответов (Markdown), превью — через универсальный санитайзер
    private fun formatDirectMatches(list: List<Article>): String {
    val unique = list.distinctBy { it.id }
    return if (unique.size == 1) {
        val art = unique.first()
        // Используем относительный путь
        val url = "/article/${art.id}"
        val preview = quillParserUtil.sanitizePreviewAnyFormat(art.description)
        listOf(
            "Нашёл статью",
            "[${art.title}]($url)",
            preview
        ).filter { it.isNotBlank() }.joinToString("\n")
    } else {
        val items = unique.joinToString("\n") { art ->
            val url = "/article/${art.id}"
            "- [${art.title}]($url)"
        }
        listOf("Я нашёл несколько релевантных статей", items).joinToString("\n")
    }
}

private fun formatSingleArticleResponse(doc: Document): String {
    val articleId = (doc.metadata["articleId"] as? Number)?.toLong() ?: 0L
    val title = doc.metadata["title"] as? String ?: "Без названия"
    // Используем относительный путь
    val url = "/article/$articleId"
    val preview = quillParserUtil.sanitizePreviewAnyFormat(doc.content)
    return listOf(
        "Нашёл статью",
        "[$title]($url)",
        preview
    ).filter { it.isNotBlank() }.joinToString("\n")
}

private fun formatMultipleArticlesResponse(documents: List<Document>): String {
    val unique = documents
        .filter { (it.metadata["articleId"] as? Number)?.toLong() != null }
        .distinctBy { (it.metadata["articleId"] as Number).toLong() }

    val items = unique.joinToString("\n") { doc ->
        val articleId = (doc.metadata["articleId"] as Number).toLong()
        val title = doc.metadata["title"] as? String ?: "Без названия"
        // Используем относительный путь
        val url = "/article/$articleId"
        "- [$title]($url)"
    }
    return listOf("Я нашёл несколько релевантных статей", "-",items).joinToString("\n")
}
}
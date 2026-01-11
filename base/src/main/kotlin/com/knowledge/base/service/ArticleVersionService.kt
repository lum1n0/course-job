package com.knowledge.base.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.dto.ArticleVersionDto
import com.knowledge.base.dto.CompareResultDto
import com.knowledge.base.dto.VersionAuthorDto
import com.knowledge.base.mapper.ArticleVersionMapper
import com.knowledge.base.model.Article
import com.knowledge.base.model.ArticleVersion
import com.knowledge.base.model.User
import com.knowledge.base.repository.ArticleProposalRepository
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.ArticleVersionRepository
import com.knowledge.base.repository.CategoryRepository
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.util.ContentDiffUtil
import com.knowledge.base.util.QuillParserUtil
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArticleVersionService(
    private val articleVersionRepository: ArticleVersionRepository,
    private val articleRepository: ArticleRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val indexingService: IndexingService,
    private val quillParserUtil: QuillParserUtil,
    private val mapper: ArticleVersionMapper,
    private val objectMapper: ObjectMapper,
    private val articleProposalRepository: ArticleProposalRepository

) {

    @Transactional
    fun createSnapshot(article: Article, editor: User, summary: String? = null): ArticleVersionDto {
        val last = articleVersionRepository.findTopByArticleIdOrderByVersionDesc(article.id)
        val next = (last?.version ?: 0) + 1
        val av = ArticleVersion(
            articleId = article.id,
            version = next,
            title = article.title,
            description = article.description,
            categoryId = article.category.id,
            videoPath = article.videoPath,
            filePath = article.filePath,
            editedById = editor.id,
            editedByEmail = editor.email,
            editedByName = "${editor.firstName} ${editor.lastName}".trim(),
            changeSummary = summary
        )
        return mapper.toDto(articleVersionRepository.save(av))
    }

    @Transactional
    fun createSnapshotSystem(article: Article, summary: String? = "bootstrap"): ArticleVersionDto {
        val last = articleVersionRepository.findTopByArticleIdOrderByVersionDesc(article.id)
        val next = (last?.version ?: 0) + 1
        val av = ArticleVersion(
            articleId = article.id,
            version = next,
            title = article.title,
            description = article.description,
            categoryId = article.category.id,
            videoPath = article.videoPath,
            filePath = article.filePath,
            editedById = null,
            editedByEmail = null,
            editedByName = null,
            changeSummary = summary
        )
        return mapper.toDto(articleVersionRepository.save(av))
    }

    @Transactional(readOnly = true)
    fun listVersions(articleId: Long): List<ArticleVersionDto> =
        articleVersionRepository.findByArticleIdOrderByVersionDesc(articleId).map { mapper.toDto(it) }

    @Transactional(readOnly = true)
    fun getVersion(articleId: Long, version: Int): ArticleVersionDto =
        mapper.toDto(
            articleVersionRepository.findByArticleIdAndVersion(articleId, version)
                .orElseThrow { IllegalArgumentException("Версия $version статьи $articleId не найдена") }
        )

    // Сравнение всегда "fromVersion" vs "текущая статья"
    @Transactional(readOnly = true)
    fun compareWithCurrent(articleId: Long, fromVersion: Int): CompareResultDto {
        val from = articleVersionRepository.findByArticleIdAndVersion(articleId, fromVersion)
            .orElseThrow { IllegalArgumentException("from=$fromVersion не найдена") }

        val article = articleRepository.findById(articleId)
            .orElseThrow { IllegalArgumentException("Статья $articleId не найдена") }

        val latestSnap = articleVersionRepository.findTopByArticleIdOrderByVersionDesc(articleId)
        val toVersionNumber = latestSnap?.version ?: 0

        // Заголовок
        val titleChanged = from.title != article.title

        // Категория
        val categoryChanged = from.categoryId != article.category.id
        val fromCategory = categoryRepository.findById(from.categoryId).orElse(null)
        val toCategory = article.category
        val categoryBeforeId = from.categoryId
        val categoryBeforeName = fromCategory?.description
        val categoryAfterId = toCategory.id
        val categoryAfterName = toCategory.description

        // Контент и вложения
        val jsonPatch = ContentDiffUtil.jsonPatch(from.description, article.description)
        val textDeltas = ContentDiffUtil.textDeltas(from.description, article.description, quillParserUtil)
        val filesDiff = ContentDiffUtil.filesDiff(from.filePath, article.filePath)
        val videosDiff = ContentDiffUtil.filesDiff(from.videoPath, article.videoPath)

        return CompareResultDto(
            fromVersion = from.version,
            toVersion = toVersionNumber,
            titleChanged = titleChanged,
            titleBefore = from.title,
            titleAfter = article.title,

            categoryChanged = categoryChanged,
            categoryBeforeId = categoryBeforeId,
            categoryBeforeName = categoryBeforeName,
            categoryAfterId = categoryAfterId,
            categoryAfterName = categoryAfterName,

            descriptionJsonPatch = jsonPatch,
            descriptionTextDeltas = textDeltas,
            filesDiff = filesDiff,
            videosDiff = videosDiff
        )
    }

    @Transactional(readOnly = true)
    fun compare(articleId: Long, fromVersion: Int, @Suppress("UNUSED_PARAMETER") toVersion: Int): CompareResultDto {
        return compareWithCurrent(articleId, fromVersion)
    }

    @Transactional
    fun restore(articleId: Long, version: Int, editorEmail: String, summary: String?): ArticleVersionDto {
        val editor = userRepository.findByEmail(editorEmail)
            ?: throw AccessDeniedException("Forbidden")
        val av = articleVersionRepository.findByArticleIdAndVersion(articleId, version)
            .orElseThrow { IllegalArgumentException("Версия $version статьи $articleId не найдена") }

        val article = articleRepository.findById(articleId)
            .orElseThrow { IllegalArgumentException("Статья $articleId не найдена") }

        val category = categoryRepository.findById(av.categoryId)
            .orElseThrow { IllegalArgumentException("Категория ${av.categoryId} не найдена") }

        val updated = article.copy(
            title = av.title,
            description = av.description,
            category = category,
            videoPath = av.videoPath,
            filePath = av.filePath
        )
        val saved = articleRepository.save(updated)
        indexingService.updateArticle(saved)

        // фиксируем откат как новую версию
        return createSnapshot(saved, editor, summary ?: "restore to v${av.version}")
    }

    @Transactional
    fun deleteVersion(articleId: Long, version: Int) {
        val versions = articleVersionRepository.findByArticleIdOrderByVersionDesc(articleId)
        if (versions.isEmpty()) throw IllegalArgumentException("Версии статьи $articleId отсутствуют")
        val latest = versions.first().version
        if (version == latest) {
            throw IllegalStateException("Нельзя удалить последнюю (текущую) версию v$latest")
        }
        if (versions.size <= 1) {
            throw IllegalStateException("Нельзя удалить последнюю оставшуюся версию статьи")
        }
        val exists = versions.any { it.version == version }
        if (!exists) throw IllegalArgumentException("Версия $version статьи $articleId не найдена")
        articleVersionRepository.deleteByArticleIdAndVersion(articleId, version)
    }

    // Инициализация: гарантировать v1 для каждого article без версий
    @Transactional
    fun ensureInitialSnapshotForAll() {
        val all = articleRepository.findAll()
        all.forEach { article ->
            val hasAny = articleVersionRepository.findTopByArticleIdOrderByVersionDesc(article.id) != null
            if (!hasAny) {
                createSnapshotSystem(article, "bootstrap")
            }
        }
    }
    @Transactional(readOnly = true)
    fun getVersionAuthor(articleId: Long, version: Int): VersionAuthorDto {
        val v = articleVersionRepository.findByArticleIdAndVersion(articleId, version)
            .orElseThrow { IllegalArgumentException("Версия $version статьи $articleId не найдена") }

        // Пытаемся извлечь id заявки из changeSummary, например "approved proposal #123"
        val summary = v.changeSummary.orEmpty()
        val match = Regex("""proposal\s*#(\d+)""", RegexOption.IGNORE_CASE).find(summary)
        if (match != null) {
            val proposalId = match.groupValues[1].toLongOrNull()
            if (proposalId != null) {
                val proposalOpt = articleProposalRepository.findById(proposalId)
                if (proposalOpt.isPresent) {
                    val p = proposalOpt.get()
                    return VersionAuthorDto(
                        id = p.authorId,
                        name = p.authorName,
                        email = p.authorEmail
                    )
                }
            }
        }

        // Фолбэк: bootstrap/restore/ручные версии — показываем того, кто зафиксировал снимок
        return VersionAuthorDto(
            id = v.editedById,
            name = v.editedByName,
            email = v.editedByEmail
        )
    }

}

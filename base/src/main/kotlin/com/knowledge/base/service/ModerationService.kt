package com.knowledge.base.service

import com.fasterxml.jackson.databind.JsonNode
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.mapper.ArticleProposalMapper
import com.knowledge.base.model.Article
import com.knowledge.base.model.ArticleProposal
import com.knowledge.base.model.ModerationStatus
import com.knowledge.base.repository.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
class ModerationService(
    private val articleRepository: ArticleRepository,
    private val articleProposalRepository: ArticleProposalRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val writerPermissionService: WriterPermissionService,
    private val moderatorPermissionService: ModeratorPermissionService, // <-- Добавлено
    private val fileStorageService: FileStorageService,
    private val indexingService: IndexingService,
    private val articleVersionService: ArticleVersionService,
    private val notificationService: NotificationService,
    private val articleProposalMapper: ArticleProposalMapper
) {

    @Transactional
    fun submitCreate(
        currentUserEmail: String,
        title: String,
        description: JsonNode?,
        categoryId: Long,
        videoFile: MultipartFile?,
        files: List<MultipartFile>?
    ): ArticleProposalDto {
        val user = userRepository.findByEmail(currentUserEmail) ?: throw AccessDeniedException("Forbidden")
        val category = categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }

        when (user.role.title) {
            "ADMIN", "MODERATOR" -> throw AccessDeniedException("Для ADMIN/MODERATOR публикация идёт напрямую, без заявок")
            "WRITER" -> {
                val allowed = writerPermissionService.checkWriterCanEditCategory(user.id, category)
                if (!allowed) throw AccessDeniedException("Forbidden")
            }
            else -> throw AccessDeniedException("Forbidden")
        }

        val storedVideo = videoFile?.let { listOf(fileStorageService.saveFile(it, "videos")) }
        val storedFiles = files?.map { fileStorageService.saveFile(it, "files") }

        val proposal = articleProposalRepository.save(
            ArticleProposal(
                articleId = null,
                finalArticleId = null,
                title = title,
                description = description,
                categoryId = category.id,
                videoPath = storedVideo,
                filePath = storedFiles,
                authorId = user.id,
                authorEmail = user.email,
                authorName = "${user.firstName} ${user.lastName}".trim(),
                status = ModerationStatus.PENDING,
                action = "CREATE",
                createdAt = Instant.now()
            )
        )
        return articleProposalMapper.toDto(proposal)
    }

    @Transactional
    fun submitUpdate(
        currentUserEmail: String,
        articleId: Long,
        title: String,
        description: JsonNode?,
        categoryId: Long,
        videoFile: MultipartFile?,
        files: List<MultipartFile>?
    ): ArticleProposalDto {
        val user = userRepository.findByEmail(currentUserEmail) ?: throw AccessDeniedException("Forbidden")
        val article = articleRepository.findById(articleId).orElseThrow { IllegalArgumentException("Статья не найдена") }
        val newCategory = categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }

        when (user.role.title) {
            "ADMIN", "MODERATOR" -> throw AccessDeniedException("Для ADMIN/MODERATOR публикация идёт напрямую, без заявок")
            "WRITER" -> {
                val allowed = writerPermissionService.checkWriterCanEditCategory(user.id, article.category)
                if (!allowed) throw AccessDeniedException("Forbidden")
            }
            else -> throw AccessDeniedException("Forbidden")
        }

        val storedVideo = videoFile?.let { listOf(fileStorageService.saveFile(it, "videos")) }
        val storedFiles = files?.map { fileStorageService.saveFile(it, "files") }

        val proposal = articleProposalRepository.save(
            ArticleProposal(
                articleId = article.id,
                finalArticleId = null,
                title = title,
                description = description,
                categoryId = newCategory.id,
                videoPath = storedVideo,
                filePath = storedFiles,
                authorId = user.id,
                authorEmail = user.email,
                authorName = "${user.firstName} ${user.lastName}".trim(),
                status = ModerationStatus.PENDING,
                action = "UPDATE",
                createdAt = Instant.now()
            )
        )
        return articleProposalMapper.toDto(proposal)
    }

    @Transactional(readOnly = true)
    fun listPending(moderatorEmail: String): List<ArticleProposalDto> {
        val moderator = userRepository.findByEmail(moderatorEmail) ?: throw AccessDeniedException("Forbidden")

        // ADMIN видит все заявки
        if (moderator.role.title == "ADMIN") {
            return articleProposalRepository.findAllByStatusOrderByCreatedAtDesc(ModerationStatus.PENDING)
                .map { articleProposalMapper.toDto(it) }
        }

        // MODERATOR видит только заявки из своих категорий
        if (moderator.role.title == "MODERATOR") {
            val allPending = articleProposalRepository.findAllByStatusOrderByCreatedAtDesc(ModerationStatus.PENDING)

            return allPending.filter { proposal ->
                val category = categoryRepository.findById(proposal.categoryId).orElse(null) ?: return@filter false
                moderatorPermissionService.checkModeratorCanEditCategory(moderator.id, category)
            }.map { articleProposalMapper.toDto(it) }
        }

        throw AccessDeniedException("Forbidden")
    }

    @Transactional(readOnly = true)
    fun listMyWork(currentUserEmail: String): List<ArticleProposalDto> {
        val user = userRepository.findByEmail(currentUserEmail) ?: throw AccessDeniedException("Forbidden")
        return articleProposalRepository.findAllByAuthorIdOrderByCreatedAtDesc(user.id)
            .map { articleProposalMapper.toDto(it) }
    }

    @Transactional
    fun approve(proposalId: Long, moderatorEmail: String, comment: String? = null): ArticleProposalDto {
        val moderator = userRepository.findByEmail(moderatorEmail) ?: throw AccessDeniedException("Forbidden")
        if (moderator.role.title != "ADMIN" && moderator.role.title != "MODERATOR") {
            throw AccessDeniedException("Forbidden")
        }

        var p = articleProposalRepository.findById(proposalId).orElseThrow { IllegalArgumentException("Заявка не найдена") }
        if (p.status != ModerationStatus.PENDING) throw IllegalStateException("Заявка уже обработана")

        // ПРОВЕРКА ДОСТУПА К КАТЕГОРИИ
        val category = categoryRepository.findById(p.categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }

        when (moderator.role.title) {
            "ADMIN" -> {} // ADMIN может всё
            "MODERATOR" -> {
                val allowed = moderatorPermissionService.checkModeratorCanEditCategory(moderator.id, category)
                if (!allowed) throw AccessDeniedException("У вас нет доступа к этой категории")
            }
        }

        val savedArticle = if (p.action == "CREATE") {
            val created = articleRepository.save(
                Article(
                    id = 0,
                    title = p.title,
                    description = p.description,
                    isDelete = false,
                    category = category,
                    videoPath = p.videoPath,
                    filePath = p.filePath
                )
            )
            created
        } else {
            val article = articleRepository.findById(p.articleId!!).orElseThrow { IllegalArgumentException("Статья не найдена") }
            val updated = article.copy(
                title = p.title,
                description = p.description,
                category = category,
                videoPath = p.videoPath ?: article.videoPath,
                filePath = p.filePath ?: article.filePath
            )
            articleRepository.save(updated)
        }

        articleVersionService.createSnapshot(savedArticle, moderator, "approved proposal #${p.id}")

        p = p.copy(
            finalArticleId = savedArticle.id,
            status = ModerationStatus.APPROVED,
            reviewedById = moderator.id,
            reviewedByEmail = moderator.email,
            reviewedByName = "${moderator.firstName} ${moderator.lastName}".trim(),
            reviewedAt = Instant.now(),
            rejectReason = comment
        )
        val savedProposal = articleProposalRepository.save(p)

        notificationService.notifyProposalApproved(savedProposal, moderator, savedArticle)

        return articleProposalMapper.toDto(savedProposal)
    }

    @Transactional
    fun reject(proposalId: Long, moderatorEmail: String, reason: String): ArticleProposalDto {
        val moderator = userRepository.findByEmail(moderatorEmail) ?: throw AccessDeniedException("Forbidden")
        if (moderator.role.title != "ADMIN" && moderator.role.title != "MODERATOR") {
            throw AccessDeniedException("Forbidden")
        }

        var p = articleProposalRepository.findById(proposalId).orElseThrow { IllegalArgumentException("Заявка не найдена") }
        if (p.status != ModerationStatus.PENDING) throw IllegalStateException("Заявка уже обработана")

        // ПРОВЕРКА ДОСТУПА К КАТЕГОРИИ
        val category = categoryRepository.findById(p.categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }

        when (moderator.role.title) {
            "ADMIN" -> {} // ADMIN может всё
            "MODERATOR" -> {
                val allowed = moderatorPermissionService.checkModeratorCanEditCategory(moderator.id, category)
                if (!allowed) throw AccessDeniedException("У вас нет доступа к этой категории")
            }
        }

        p = p.copy(
            status = ModerationStatus.REJECTED,
            reviewedById = moderator.id,
            reviewedByEmail = moderator.email,
            reviewedByName = "${moderator.firstName} ${moderator.lastName}".trim(),
            reviewedAt = Instant.now(),
            rejectReason = reason
        )
        val savedProposal = articleProposalRepository.save(p)

        notificationService.notifyProposalRejected(savedProposal, moderator, reason)

        return articleProposalMapper.toDto(savedProposal)
    }

    @Transactional(readOnly = true)
    fun getProposal(proposalId: Long, moderatorEmail: String): ArticleProposalDto {
        val moderator = userRepository.findByEmail(moderatorEmail) ?: throw AccessDeniedException("Forbidden")

        val proposal = articleProposalRepository.findById(proposalId).orElseThrow { IllegalArgumentException("Заявка не найдена") }

        // ПРОВЕРКА ДОСТУПА К КАТЕГОРИИ
        if (moderator.role.title == "MODERATOR") {
            val category = categoryRepository.findById(proposal.categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }
            val allowed = moderatorPermissionService.checkModeratorCanEditCategory(moderator.id, category)
            if (!allowed) throw AccessDeniedException("У вас нет доступа к этой категории")
        }

        return articleProposalMapper.toDto(proposal)
    }

    @Transactional
    fun submitPublicCreate(
        currentUserEmail: String,
        title: String,
        description: JsonNode?,
        categoryId: Long,
        videoFile: MultipartFile?,
        files: List<MultipartFile>?
    ): ArticleProposalDto {
        val user = userRepository.findByEmail(currentUserEmail) ?: throw AccessDeniedException("Forbidden")

        if (user.role.title == "WRITER") {
            throw AccessDeniedException("Writers must use writer flow")
        }

        val category = categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Категория не найдена") }

        val isPublicCategory = category.accessRoles.any { it.title == "PUBLIC" }
        if (!isPublicCategory) {
            throw AccessDeniedException("Category is not PUBLIC")
        }

        val storedVideo = videoFile?.let { listOf(fileStorageService.saveFile(it, "videos")) }
        val storedFiles = files?.map { fileStorageService.saveFile(it, "files") }

        val proposal = articleProposalRepository.save(
            ArticleProposal(
                articleId = null,
                finalArticleId = null,
                title = title,
                description = description,
                categoryId = category.id,
                videoPath = storedVideo,
                filePath = storedFiles,
                authorId = user.id,
                authorEmail = user.email,
                authorName = "${user.firstName} ${user.lastName}".trim(),
                status = ModerationStatus.PENDING,
                action = "CREATE",
                createdAt = Instant.now()
            )
        )

        return articleProposalMapper.toDto(proposal)
    }
}

package com.knowledge.base.functional

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.dto.ArticleProposalDto
import com.knowledge.base.mapper.ArticleMapper
import com.knowledge.base.mapper.ArticleProposalMapper
import com.knowledge.base.mapper.CategoryMapper
import com.knowledge.base.model.*
import com.knowledge.base.repository.*
import com.knowledge.base.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.*

// Вспомогательная функция для Mockito any() в Kotlin
private inline fun <reified T> anyNonNull(): T = org.mockito.Mockito.any(T::class.java) ?: createInstance()
private inline fun <reified T> eqNonNull(value: T): T {
    org.mockito.Mockito.eq(value)
    return value
}

private inline fun <reified T> createInstance(): T = when (T::class) {
    Article::class -> Article(
        id = 0,
        title = "",
        category = Category(id = 0, description = ""),
        isDelete = false,
        videoPath = null
    ) as T

    ArticleProposal::class -> ArticleProposal(
        id = 0,
        title = "",
        description = null,
        categoryId = 0,
        authorId = 0,
        authorEmail = "",
        status = ModerationStatus.PENDING,
        action = "",
        createdAt = Instant.now()
    ) as T

    User::class -> User(id = 0, email = "", role = Role(title = ""), firstName = "", lastName = "") as T
    else -> throw IllegalArgumentException("Add createInstance case for ${T::class}")
}

@ExtendWith(MockitoExtension::class)
class SearchAndModerationTest {


    @Mock
    lateinit var articleRepository: ArticleRepository
    @Mock
    lateinit var articleProposalRepository: ArticleProposalRepository
    @Mock
    lateinit var categoryRepository: CategoryRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var writerPermissionService: WriterPermissionService
    @Mock
    lateinit var moderatorPermissionService: ModeratorPermissionService
    @Mock
    lateinit var fileStorageService: FileStorageService
    @Mock
    lateinit var indexingService: IndexingService
    @Mock
    lateinit var articleVersionService: ArticleVersionService
    @Mock
    lateinit var notificationService: NotificationService
    @Mock
    lateinit var articleProposalMapper: ArticleProposalMapper
    @Mock
    lateinit var accessRoleRepository: AccessRoleRepository
    @Mock
    lateinit var articleMapper: ArticleMapper
    @Mock
    lateinit var categoryMapper: CategoryMapper
    @Mock
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper
    @Mock
    lateinit var articleViewHitRepository: ArticleViewHitRepository
    @Mock
    lateinit var htmlToDeltaConverter: com.knowledge.base.util.HtmlToDeltaConverter


    @InjectMocks
    lateinit var moderationService: ModerationService

    @InjectMocks
    lateinit var articleService: ArticleService

    private lateinit var adminUser: User
    private lateinit var writerUser: User
    private lateinit var testCategory: Category
    private lateinit var proposal: ArticleProposal

    @BeforeEach
    fun setUp() {
        testCategory = Category(id = 1L, description = "Tech")
        adminUser = User(
            id = 1L,
            email = "admin@example.com",
            role = Role(title = "ADMIN"),
            firstName = "Admin",
            lastName = "User"
        )
        writerUser = User(
            id = 2L,
            email = "writer@example.com",
            role = Role(title = "WRITER"),
            firstName = "John",
            lastName = "Doe"
        )

        proposal = ArticleProposal(
            id = 10L,
            title = "New Article",
            description = JsonNodeFactory.instance.textNode("Content"),
            categoryId = 1L,
            authorId = 2L,
            authorEmail = "writer@example.com",
            status = ModerationStatus.PENDING,
            action = "CREATE",
            createdAt = Instant.now()
        )
    }

    @Test
    fun `Moderation Flow - Admin approves proposal`() {
        // Подготовка
        `when`(userRepository.findByEmail("admin@example.com")).thenReturn(adminUser)
        `when`(articleProposalRepository.findById(10L)).thenReturn(Optional.of(proposal))
        `when`(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory))

        val savedArticle =
            Article(id = 100L, title = "New Article", category = testCategory, isDelete = false, videoPath = null)
        `when`(articleRepository.save(anyNonNull<Article>())).thenReturn(savedArticle)

        `when`(articleProposalRepository.save(anyNonNull<ArticleProposal>())).thenAnswer { it.arguments[0] }

        val proposalDto = ArticleProposalDto(
            id = 10L,
            articleId = null,
            finalArticleId = 100L,
            title = "New Article",
            description = JsonNodeFactory.instance.textNode("Content"),
            categoryId = 1L,
            videoPath = null,
            filePath = null,
            authorId = 2L,
            authorEmail = "writer@example.com",
            authorName = "John Doe",
            status = ModerationStatus.APPROVED,
            reviewedById = 1L,
            reviewedByEmail = "admin@example.com",
            reviewedByName = "Admin User",
            reviewedAt = Instant.now(),
            rejectReason = null,
            action = "CREATE",
            createdAt = Instant.now()
        )
        `when`(articleProposalMapper.toDto(anyNonNull<ArticleProposal>())).thenReturn(proposalDto)

        // Выполнение
        val result = moderationService.approve(10L, "admin@example.com")

        // Проверка
        assertEquals(ModerationStatus.APPROVED, result.status)
        assertEquals(100L, result.finalArticleId)

        verify(articleRepository).save(anyNonNull<Article>())
        verify(notificationService).notifyProposalApproved(anyNonNull(), eqNonNull(adminUser), eqNonNull(savedArticle))
    }

    @Test
    fun `Search Flow - Search Guest Articles`() {
        // Подготовка для ArticleService (частично переиспользуемые моки)
        val guestRole = AccessRole(id = 5L, title = "GUEST")
        `when`(accessRoleRepository.findByTitle("GUEST")).thenReturn(guestRole)

        val guestCategory = Category(id = 2L, description = "Public", accessRoles = mutableListOf(guestRole))
        `when`(categoryRepository.findAllByIsDeleteFalse()).thenReturn(listOf(guestCategory))

        val article =
            Article(id = 200L, title = "Public Info", category = guestCategory, isDelete = false, videoPath = null)
        `when`(articleRepository.findByTitleContainingIgnoreCaseAndIsDeleteFalse("Info")).thenReturn(listOf(article))

        val articleDto = ArticleDto(id = 200L, title = "Public Info")
        `when`(articleMapper.toDto(article)).thenReturn(articleDto)

        // Выполнение
        val result = articleService.searchGuestArticles("Info")

        // Проверка
        assertEquals(1, result.size)
        assertEquals("Public Info", result[0].title)
    }
}

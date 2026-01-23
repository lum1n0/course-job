package com.knowledge.base.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.mapper.ArticleMapper
import com.knowledge.base.mapper.CategoryMapper
import com.knowledge.base.model.Article
import com.knowledge.base.model.Category
import com.knowledge.base.model.Role
import com.knowledge.base.model.User
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.ArticleViewHitRepository
import com.knowledge.base.repository.CategoryRepository
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.util.HtmlToDeltaConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.access.AccessDeniedException
import java.util.*
import org.mockito.ArgumentMatchers

// Helper function to avoid NPE with Mockito matchers in Kotlin
fun <T> anyNonNull(): T = ArgumentMatchers.any()

@ExtendWith(MockitoExtension::class)
class ArticleServiceTest {

    @Mock lateinit var articleRepository: ArticleRepository
    @Mock lateinit var categoryRepository: CategoryRepository
    @Mock lateinit var articleMapper: ArticleMapper
    @Mock lateinit var categoryMapper: CategoryMapper
    @Mock lateinit var fileStorageService: FileStorageService
    @Mock lateinit var accessRoleRepository: AccessRoleRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var writerPermissionService: WriterPermissionService
    @Mock lateinit var indexingService: IndexingService
    @Mock lateinit var objectMapper: ObjectMapper
    @Mock lateinit var articleViewHitRepository: ArticleViewHitRepository
    @Mock lateinit var articleVersionService: ArticleVersionService
    @Mock lateinit var notificationService: NotificationService
    @Mock lateinit var htmlToDeltaConverter: HtmlToDeltaConverter
    @Mock lateinit var moderatorPermissionService: ModeratorPermissionService

    @InjectMocks
    lateinit var articleService: ArticleService

    private lateinit var sampleArticle: Article
    private lateinit var sampleArticleDto: ArticleDto
    private lateinit var sampleCategory: Category
    private lateinit var sampleUser: User

    @BeforeEach
    fun setUp() {
        sampleCategory = Category(id = 1L, description = "Tech")
        sampleArticle = Article(
            id = 100L,
            title = "Intro to Kotlin",
            description = JsonNodeFactory.instance.textNode("Content"),
            category = sampleCategory,
            isDelete = false,
            videoPath = null
        )
        sampleArticleDto = ArticleDto(
            id = 100L,
            title = "Intro to Kotlin",
            categoryDto = CategoryDto(id = 1L, description = "Tech")
        )

        sampleUser = User(
            id = 1L,
            email = "test@example.com",
            role = Role(id = 1L, title = "ADMIN")
        )
    }

    @Test
    fun `getArticleById returns DTO when article exists`() {
        // Подготовка
        `when`(articleRepository.findById(100L)).thenReturn(Optional.of(sampleArticle))
        `when`(articleMapper.toDto(sampleArticle)).thenReturn(sampleArticleDto)

        // Выполнение
        val result = articleService.getArticleById(100L)

        // Проверка
        assertNotNull(result)
        assertEquals(100L, result?.id)
        verify(articleRepository).findById(100L)
    }

    @Test
    fun `getArticleById returns null when article does not exist`() {
        // Подготовка
        `when`(articleRepository.findById(999L)).thenReturn(Optional.empty())

        // Выполнение
        val result = articleService.getArticleById(999L)

        // Проверка
        assertNull(result)
    }

    @Test
    fun `softDeleteArticle marks article as deleted and creates snapshot`() {
        // Подготовка
        val email = "test@example.com"
        val deletedArticle = sampleArticle.copy(isDelete = true)
        
        `when`(userRepository.findByEmail(email)).thenReturn(sampleUser)
        `when`(articleRepository.findById(100L)).thenReturn(Optional.of(sampleArticle))
        `when`(articleRepository.save(anyNonNull())).thenReturn(deletedArticle)
        `when`(articleMapper.toDto(deletedArticle)).thenReturn(sampleArticleDto.copy(isDelete = true))

        // Выполнение
        val result = articleService.softDeleteArticle(email, 100L, true)

        // Проверка
        assertTrue(result.isDelete)
        verify(articleVersionService, times(1)).createSnapshot(anyNonNull(), anyNonNull(), anyNonNull())
        verify(indexingService, times(1)).indexArticleById(100L)
    }

    @Test
    fun `softDeleteArticle throws Forbidden if user not found`() {
        // Подготовка
        `when`(userRepository.findByEmail("unknown@example.com")).thenReturn(null)

        // Выполнение и Проверка
        assertThrows(AccessDeniedException::class.java) {
            articleService.softDeleteArticle("unknown@example.com", 100L, true)
        }
    }
}

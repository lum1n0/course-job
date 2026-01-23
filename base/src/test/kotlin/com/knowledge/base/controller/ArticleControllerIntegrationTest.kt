package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.service.ArticleService
import com.knowledge.base.service.ArticleViewService
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.FileStorageService
import com.knowledge.base.service.PDFService
import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [ArticleController::class]
)
class ArticleControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var articleService: ArticleService

    @MockBean
    private lateinit var articleViewService: ArticleViewService

    @MockBean
    private lateinit var jwtUtil: JwtUtil

    @MockBean
    private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl
    
    @MockBean
    private lateinit var baseLdapPathContextSource: BaseLdapPathContextSource

    @MockBean
    private lateinit var fileStorageService: FileStorageService

    @MockBean
    private lateinit var pdfService: PDFService

    @MockBean
    private lateinit var categoryService: CategoryService

    @Test
    @WithMockUser(username = "user", roles = ["WRITER"])
    fun `getArticleById returns article when exists`() {
        val articleId = 1L
        val articleDto = ArticleDto(
            id = articleId,
            title = "Integration Test Article",
            categoryDto = CategoryDto(id = 10L, description = "Test Cat")
        )

        `when`(articleService.getArticleForUserById(articleId, "user")).thenReturn(articleDto)

        mockMvc.perform(get("/api/articles/{id}", articleId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(articleId))
            .andExpect(jsonPath("$.title").value("Integration Test Article"))
    }
    
    @Test
    @WithMockUser(username = "user", roles = ["WRITER"])
    fun `getArticleById returns 404 when not found`() {
        val articleId = 999L
        
        `when`(articleService.getArticleForUserById(articleId, "user")).thenReturn(null)

        mockMvc.perform(get("/api/articles/{id}", articleId))
            .andExpect(status().isNotFound)
    }
}

package com.knowledge.base.security

import com.knowledge.base.service.ArticleService
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.UserDetailsServiceImpl
import com.knowledge.base.util.JwtUtil
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var ldapContextSource: LdapContextSource

    @MockBean
    private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    @MockBean
    private lateinit var jwtUtil: JwtUtil

    @MockBean
    private lateinit var articleService: ArticleService

    @MockBean
    private lateinit var categoryService: CategoryService

    @Test
    fun `login endpoint is public`() {
        mockMvc.perform(post("/api/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect { result ->
                assertNotEquals(403, result.response.status, "Эндпоинт должен быть публичным")
            }
    }

    @Test
    fun `articles endpoint requires authentication`() {
        mockMvc.perform(get("/api/articles/by-category"))
            .andExpect(status().isForbidden) // Без пользователя должен вернуться статус 403/401. По умолчанию Spring Security возвращает 403 или 401 в зависимости от конфигурации.
            // В стандартной настройке неаутентифицированный доступ к защищенному ресурсу возвращает 401/403.
            // Проверяем, что возвращается 403 (Запрещено) или 401 (Не авторизован).
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `admin can access protected article endpoint`() {
        // Этот эндпоинт требует роль ADMIN
        mockMvc.perform(get("/api/articles/all"))
            // Ожидаем 200 OK или 400/500 если контроллер не сработает, но НЕ 403.
            // Поскольку мы замокировали зависимости, контроллер может вернуть 500 или NPE если мы не замокировали все.
            // Но в первую очередь мы хотим протестировать Security.
            // Если security пропускает, запрос попадает в контроллер.
            .andExpect(status().isOk) // Предполагаем, что мы замокировали зависимости контроллера в реальном сценарии,
            // или ожидаем 500, что означает "Доступ предоставлен, но выполнение не удалось".
            // 403 означал бы "Доступ запрещен".
    }

    @Test
    @WithMockUser(username = "user", roles = ["WRITER"])
    fun `writer cannot access admin article endpoint`() {
        mockMvc.perform(get("/api/articles/all"))
            .andExpect(status().isForbidden)
    }
}

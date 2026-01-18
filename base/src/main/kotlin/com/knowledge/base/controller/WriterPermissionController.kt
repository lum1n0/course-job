package com.knowledge.base.controller

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.WriterPermissionDto
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.UserService
import com.knowledge.base.service.WriterPermissionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/writer-permissions")
@Tag(name = "Writer Permissions", description = "Управление правами авторов")
class WriterPermissionController(
    private val writerPermissionService: WriterPermissionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository,
    private val userService: UserService
) {

    @Operation(
        summary = "Выдать права автора",
        description = "Выдает права автора на роль доступа (только для ADMIN)",
        parameters = [
            Parameter(name = "writerId", description = "ID автора", required = true, `in` = ParameterIn.QUERY),
            Parameter(name = "accessRoleId", description = "ID роли доступа", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Права успешно выданы",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = WriterPermissionDto::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    fun grant(
        @Parameter(description = "ID автора", required = true) @RequestParam writerId: Long,
        @Parameter(description = "ID роли доступа", required = true) @RequestParam accessRoleId: Long
    ): ResponseEntity<WriterPermissionDto> {
        val dto = writerPermissionService.grant(writerId, accessRoleId)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Отозвать права автора",
        description = "Отзывает права автора на роль доступа (только для ADMIN)",
        parameters = [
            Parameter(name = "writerId", description = "ID автора", required = true, `in` = ParameterIn.QUERY),
            Parameter(name = "accessRoleId", description = "ID роли доступа", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Права успешно отозваны"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Права не найдены")
        ]
    )
    @DeleteMapping("/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    fun revoke(
        @Parameter(description = "ID автора", required = true) @RequestParam writerId: Long,
        @Parameter(description = "ID роли доступа", required = true) @RequestParam accessRoleId: Long
    ): ResponseEntity<Void> {
        writerPermissionService.revoke(writerId, accessRoleId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Получить права автора",
        description = "Возвращает список прав указанного автора (только для ADMIN)",
        parameters = [
            Parameter(name = "writerId", description = "ID автора", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список прав успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/by-writer/{writerId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun listForWriter(@Parameter(description = "ID автора", required = true) @PathVariable writerId: Long): ResponseEntity<List<WriterPermissionDto>> {
        return ResponseEntity.ok(writerPermissionService.listForWriter(writerId))
    }

    @Operation(
        summary = "Проверить права на редактирование категории",
        description = "Проверяет, может ли текущий пользователь редактировать указанную категорию",
        parameters = [
            Parameter(name = "categoryId", description = "ID категории", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Результат проверки получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Boolean::class)
                )]
            )
        ]
    )
    @GetMapping("/me/can-edit")
    @PreAuthorize("isAuthenticated()")
    fun meCanEdit(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID категории", required = true) @RequestParam categoryId: Long
    ): ResponseEntity<Boolean> {
        val currentUsername = authentication.name
        val user = userRepository.findByEmail(currentUsername) ?: return ResponseEntity.ok(false)
        val category = categoryService.getCategoryEntityById(categoryId) ?: return ResponseEntity.ok(false)
        val allowed = when (user.role.title) {
            "ADMIN" -> true
            "WRITER" -> writerPermissionService.checkWriterCanEditCategory(user.id, category)
            else -> false
        }
        return ResponseEntity.ok(allowed)
    }

    @Operation(
        summary = "Получить доступные категории для текущего автора",
        description = "Возвращает список категорий, которые текущий автор может редактировать",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список категорий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/me/categories-editable")
    @PreAuthorize("isAuthenticated()")
    fun meEditableCategories(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<CategoryDto>> {
        val currentUsername = authentication.name
        val user = userRepository.findByEmail(currentUsername) ?: return ResponseEntity.ok(emptyList())
        return ResponseEntity.ok(categoryService.getEditableCategoriesForWriter(user.id))
    }

    @Operation(
        summary = "Получить доступные категории для автора",
        description = "Возвращает список категорий, которые указанный автор может редактировать (только для ADMIN)",
        parameters = [
            Parameter(name = "writerId", description = "ID автора", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список категорий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/{writerId}/categories-editable")
    @PreAuthorize("hasRole('ADMIN')")
    fun editableCategoriesForWriter(@Parameter(description = "ID автора", required = true) @PathVariable writerId: Long): ResponseEntity<List<CategoryDto>> {
        return ResponseEntity.ok(categoryService.getEditableCategoriesForWriter(writerId))
    }
}

package com.knowledge.base.controller

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.ModeratorPermissionDto
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.ModeratorPermissionService
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
@RequestMapping("/api/moderator-permissions")
@Tag(name = "Moderator Permissions", description = "Управление правами модераторов")
class ModeratorPermissionController(
    private val moderatorPermissionService: ModeratorPermissionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    @Operation(
        summary = "Выдать права модератора",
        description = "Выдает права модератора на роль доступа (только для ADMIN)",
        parameters = [
            Parameter(name = "moderatorId", description = "ID модератора", required = true, `in` = ParameterIn.QUERY),
            Parameter(name = "accessRoleId", description = "ID роли доступа", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Права успешно выданы",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ModeratorPermissionDto::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    fun grant(
        @Parameter(description = "ID модератора", required = true) @RequestParam moderatorId: Long,
        @Parameter(description = "ID роли доступа", required = true) @RequestParam accessRoleId: Long
    ): ResponseEntity<ModeratorPermissionDto> {
        val dto = moderatorPermissionService.grant(moderatorId, accessRoleId)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Отозвать права модератора",
        description = "Отзывает права модератора на роль доступа (только для ADMIN)",
        parameters = [
            Parameter(name = "moderatorId", description = "ID модератора", required = true, `in` = ParameterIn.QUERY),
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
        @Parameter(description = "ID модератора", required = true) @RequestParam moderatorId: Long,
        @Parameter(description = "ID роли доступа", required = true) @RequestParam accessRoleId: Long
    ): ResponseEntity<Void> {
        moderatorPermissionService.revoke(moderatorId, accessRoleId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Получить права модератора",
        description = "Возвращает список прав указанного модератора (только для ADMIN)",
        parameters = [
            Parameter(name = "moderatorId", description = "ID модератора", required = true, `in` = ParameterIn.PATH)
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
    @GetMapping("/by-moderator/{moderatorId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun listForModerator(@Parameter(description = "ID модератора", required = true) @PathVariable moderatorId: Long): ResponseEntity<List<ModeratorPermissionDto>> {
        return ResponseEntity.ok(moderatorPermissionService.listForModerator(moderatorId))
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
        val user = userRepository.findByEmail(currentUsername)
            ?: return ResponseEntity.ok(false)
        val category = categoryService.getCategoryEntityById(categoryId)
            ?: return ResponseEntity.ok(false)
        val allowed = when (user.role.title) {
            "ADMIN" -> true
            "MODERATOR" -> moderatorPermissionService.checkModeratorCanEditCategory(user.id, category)
            "WRITER" -> false
            else -> false
        }
        return ResponseEntity.ok(allowed)
    }

    @Operation(
        summary = "Получить доступные категории для текущего модератора",
        description = "Возвращает список категорий, которые текущий модератор может редактировать",
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
        val user = userRepository.findByEmail(currentUsername)
            ?: return ResponseEntity.ok(emptyList())
        return ResponseEntity.ok(categoryService.getEditableCategoriesForModerator(user.id))
    }

    @Operation(
        summary = "Получить доступные категории для модератора",
        description = "Возвращает список категорий, которые указанный модератор может редактировать (только для ADMIN)",
        parameters = [
            Parameter(name = "moderatorId", description = "ID модератора", required = true, `in` = ParameterIn.PATH)
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
    @GetMapping("/{moderatorId}/categories-editable")
    @PreAuthorize("hasRole('ADMIN')")
    fun editableCategoriesForModerator(@Parameter(description = "ID модератора", required = true) @PathVariable moderatorId: Long): ResponseEntity<List<CategoryDto>> {
        return ResponseEntity.ok(categoryService.getEditableCategoriesForModerator(moderatorId))
    }
}

package com.knowledge.base.controller

import com.knowledge.base.dto.AccessRoleDto
import com.knowledge.base.service.AccessRoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/access-role")
@Tag(name = "Access Roles", description = "Управление ролями доступа")
class AccessRoleController(private val accessRoleService: AccessRoleService) {

    @Operation(
        summary = "Получить все роли доступа",
        description = "Возвращает список ролей доступа, видимых для текущего пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список ролей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    fun getAllAccessRole(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<AccessRoleDto>> {
        val result = accessRoleService.getAccessRolesVisibleForUser(authentication.name)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Получить роль доступа по названию",
        description = "Возвращает роль доступа с указанным названием",
        parameters = [
            Parameter(name = "title", description = "Название роли доступа", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Роль успешно найдена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AccessRoleDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Роль не найдена")
        ]
    )
    @GetMapping
    fun getAccessRoleByTitle(@Parameter(description = "Название роли доступа", required = true) @RequestParam title: String): ResponseEntity<AccessRoleDto> {
        val accessRole = accessRoleService.findAccessRoleByTitle(title)
        return ResponseEntity.ok(accessRole)
    }

    @Operation(
        summary = "Проверить доступ пользователя к роли",
        description = "Проверяет, имеет ли пользователь указанную роль доступа",
        parameters = [
            Parameter(name = "userId", description = "ID пользователя", required = true, `in` = ParameterIn.QUERY),
            Parameter(name = "accessRoleTitle", description = "Название роли доступа", required = true, `in` = ParameterIn.QUERY)
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
    @GetMapping("/full/user-has-access")
    fun checkUserHasAccessRole(
        @Parameter(description = "ID пользователя", required = true) @RequestParam userId: Long,
        @Parameter(description = "Название роли доступа", required = true) @RequestParam accessRoleTitle: String
    ): ResponseEntity<Boolean> {
        val hasAccess = accessRoleService.checkUserHasAccessRole(userId, accessRoleTitle)
        return ResponseEntity.ok(hasAccess)
    }

    @Operation(
        summary = "Создать роль доступа",
        description = "Создает новую роль доступа",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для создания роли",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = AccessRoleDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Роль успешно создана",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AccessRoleDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные")
        ]
    )
    @PostMapping
    fun createAccessRole(@RequestBody accessRoleDto: AccessRoleDto): ResponseEntity<AccessRoleDto> {
        val newAccessRole = accessRoleService.createAccessRole(accessRoleDto)
        return ResponseEntity.ok(newAccessRole)
    }

    @Operation(
        summary = "Обновить роль доступа",
        description = "Обновляет существующую роль доступа",
        parameters = [
            Parameter(name = "id", description = "ID роли доступа", required = true, `in` = ParameterIn.PATH)
        ],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для обновления роли",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = AccessRoleDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Роль успешно обновлена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AccessRoleDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Роль не найдена")
        ]
    )
    @PutMapping("/{id}")
    fun updateAccessRole(
        @Parameter(description = "ID роли доступа", required = true) @PathVariable id: Long,
        @RequestBody accessRoleDto: AccessRoleDto
    ): ResponseEntity<AccessRoleDto> {
        val updated = accessRoleService.updateAccessRole(id, accessRoleDto)
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Удалить роль доступа",
        description = "Удаляет роль доступа по ID",
        parameters = [
            Parameter(name = "id", description = "ID роли доступа", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Роль успешно удалена"),
            ApiResponse(responseCode = "404", description = "Роль не найдена")
        ]
    )
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccessRole(@Parameter(description = "ID роли доступа", required = true) @PathVariable id: Long) {
        accessRoleService.deleteAccessRoleById(id)
    }
}

package com.knowledge.base.controller

import com.knowledge.base.dto.UserDto
import com.knowledge.base.dto.FavoriteDto
import com.knowledge.base.service.FavoriteService
import com.knowledge.base.service.UserService
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
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Управление профилем пользователя")
class ProfileController(
    private val userService: UserService,
    private val favoriteService: FavoriteService
) {

    @Operation(
        summary = "Получить данные профиля",
        description = "Возвращает личные данные текущего пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Данные профиля успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Пользователь не найден")
        ]
    )
    @GetMapping("/my-date")
    @PreAuthorize("isAuthenticated()")
    fun myData(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<UserDto> {
        val dto = userService.findByEmail(authentication.name) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Получить избранные статьи",
        description = "Возвращает список избранных статей текущего пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список избранного успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    fun myFavorites(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<FavoriteDto>> {
        return ResponseEntity.ok(favoriteService.list(authentication.name))
    }

    @Operation(
        summary = "Добавить в избранное",
        description = "Добавляет статью в избранное текущего пользователя",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно добавлена в избранное",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FavoriteDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @PostMapping("/favorites/{articleId}")
    @PreAuthorize("isAuthenticated()")
    fun addFavorite(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long
    ): ResponseEntity<FavoriteDto> {
        return ResponseEntity.ok(favoriteService.add(authentication.name, articleId))
    }

    @Operation(
        summary = "Удалить из избранного",
        description = "Удаляет статью из избранного текущего пользователя",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Статья успешно удалена из избранного"),
            ApiResponse(responseCode = "404", description = "Статья не найдена в избранном")
        ]
    )
    @DeleteMapping("/favorites/{articleId}")
    @PreAuthorize("isAuthenticated()")
    fun removeFavorite(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long
    ): ResponseEntity<Void> {
        favoriteService.remove(authentication.name, articleId)
        return ResponseEntity.noContent().build()
    }
}

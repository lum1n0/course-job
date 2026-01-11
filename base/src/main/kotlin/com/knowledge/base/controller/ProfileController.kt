// src/main/kotlin/com/knowledge/base/controller/ProfileController.kt
package com.knowledge.base.controller

import com.knowledge.base.dto.UserDto
import com.knowledge.base.dto.FavoriteDto
import com.knowledge.base.service.FavoriteService
import com.knowledge.base.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val userService: UserService,
    private val favoriteService: FavoriteService
) {

    // 1) Личные данные пользователя
    @GetMapping("/my-date")
    @PreAuthorize("isAuthenticated()")
    fun myData(authentication: Authentication): ResponseEntity<UserDto> {
        val dto = userService.findByEmail(authentication.name) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dto)
    }

    // 2) Избранное — список
    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    fun myFavorites(authentication: Authentication): ResponseEntity<List<FavoriteDto>> {
        return ResponseEntity.ok(favoriteService.list(authentication.name))
    }

    // 2) Избранное — добавить
    @PostMapping("/favorites/{articleId}")
    @PreAuthorize("isAuthenticated()")
    fun addFavorite(authentication: Authentication, @PathVariable articleId: Long): ResponseEntity<FavoriteDto> {
        return ResponseEntity.ok(favoriteService.add(authentication.name, articleId))
    }

    // 2) Избранное — удалить
    @DeleteMapping("/favorites/{articleId}")
    @PreAuthorize("isAuthenticated()")
    fun removeFavorite(authentication: Authentication, @PathVariable articleId: Long): ResponseEntity<Void> {
        favoriteService.remove(authentication.name, articleId)
        return ResponseEntity.noContent().build()
    }
}

package com.knowledge.base.controller

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.ModeratorPermissionDto
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.ModeratorPermissionService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/moderator-permissions")
class ModeratorPermissionController(
    private val moderatorPermissionService: ModeratorPermissionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {

    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    fun grant(
        @RequestParam moderatorId: Long,
        @RequestParam accessRoleId: Long
    ): ResponseEntity<ModeratorPermissionDto> {
        val dto = moderatorPermissionService.grant(moderatorId, accessRoleId)
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    fun revoke(
        @RequestParam moderatorId: Long,
        @RequestParam accessRoleId: Long
    ): ResponseEntity<Void> {
        moderatorPermissionService.revoke(moderatorId, accessRoleId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/by-moderator/{moderatorId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun listForModerator(@PathVariable moderatorId: Long): ResponseEntity<List<ModeratorPermissionDto>> {
        return ResponseEntity.ok(moderatorPermissionService.listForModerator(moderatorId))
    }

    @GetMapping("/me/can-edit")
    @PreAuthorize("isAuthenticated()")
    fun meCanEdit(
        authentication: Authentication,
        @RequestParam categoryId: Long
    ): ResponseEntity<Boolean> {
        val currentUsername = authentication.name
        val user = userRepository.findByEmail(currentUsername)
            ?: return ResponseEntity.ok(false)

        val category = categoryService.getCategoryEntityById(categoryId)
            ?: return ResponseEntity.ok(false)

        val allowed = when (user.role.title) {
            "ADMIN" -> true
            "MODERATOR" -> moderatorPermissionService.checkModeratorCanEditCategory(user.id, category)
            "WRITER" -> false // У writer свои проверки через WriterPermission
            else -> false
        }

        return ResponseEntity.ok(allowed)
    }

    @GetMapping("/me/categories-editable")
    @PreAuthorize("isAuthenticated()")
    fun meEditableCategories(authentication: Authentication): ResponseEntity<List<CategoryDto>> {
        val currentUsername = authentication.name
        val user = userRepository.findByEmail(currentUsername)
            ?: return ResponseEntity.ok(emptyList())

        return ResponseEntity.ok(categoryService.getEditableCategoriesForModerator(user.id))
    }

    @GetMapping("/{moderatorId}/categories-editable")
    @PreAuthorize("hasRole('ADMIN')")
    fun editableCategoriesForModerator(@PathVariable moderatorId: Long): ResponseEntity<List<CategoryDto>> {
        return ResponseEntity.ok(categoryService.getEditableCategoriesForModerator(moderatorId))
    }
}

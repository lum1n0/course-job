package com.knowledge.base.controller

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.WriterPermissionDto
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.UserService
import com.knowledge.base.service.WriterPermissionService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/writer-permissions")
class WriterPermissionController(
    private val writerPermissionService: WriterPermissionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository,
    private val userService: UserService
) {

    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    fun grant(
        @RequestParam writerId: Long,
        @RequestParam accessRoleId: Long
    ): ResponseEntity<WriterPermissionDto> {
        val dto = writerPermissionService.grant(writerId, accessRoleId)
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    fun revoke(
        @RequestParam writerId: Long,
        @RequestParam accessRoleId: Long
    ): ResponseEntity<Void> {
        writerPermissionService.revoke(writerId, accessRoleId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/by-writer/{writerId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun listForWriter(@PathVariable writerId: Long): ResponseEntity<List<WriterPermissionDto>> {
        return ResponseEntity.ok(writerPermissionService.listForWriter(writerId))
    }

    @GetMapping("/me/can-edit")
    @PreAuthorize("isAuthenticated()")
    fun meCanEdit(
        authentication: Authentication,
        @RequestParam categoryId: Long
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

    // НОВОЕ: список категорий, которые ТЕКУЩИЙ пользователь (WRITER) может редактировать
    @GetMapping("/me/categories-editable")
    @PreAuthorize("isAuthenticated()")
    fun meEditableCategories(authentication: Authentication): ResponseEntity<List<CategoryDto>> {
        val currentUsername = authentication.name
        val user = userRepository.findByEmail(currentUsername) ?: return ResponseEntity.ok(emptyList())
        return ResponseEntity.ok(categoryService.getEditableCategoriesForWriter(user.id))
    }

    // НОВОЕ: список категорий, которые указанный WRITER может редактировать (ADMIN-only)
    @GetMapping("/{writerId}/categories-editable")
    @PreAuthorize("hasRole('ADMIN')")
    fun editableCategoriesForWriter(@PathVariable writerId: Long): ResponseEntity<List<CategoryDto>> {
        return ResponseEntity.ok(categoryService.getEditableCategoriesForWriter(writerId))
    }
}

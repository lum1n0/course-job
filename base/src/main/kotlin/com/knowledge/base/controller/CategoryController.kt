package com.knowledge.base.controller;

import com.knowledge.base.dto.ArticleDto;
import com.knowledge.base.dto.CategoryContentDto;
import com.knowledge.base.dto.CategoryDto;
import com.knowledge.base.dto.CategoryTreeDto;
import com.knowledge.base.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
class CategoryController(private val categoryService: CategoryService) {

    @GetMapping("/all")
    fun getAllCategoryForAdmin(pageable: Pageable): ResponseEntity<Page<CategoryDto>> {
        val category = categoryService.getAllCategory(pageable)
        return ResponseEntity.ok(category)
    }

    @GetMapping("/{userId}/for-user-all")
    @PreAuthorize("isAuthenticated()")
    fun getAllCategoryForUsers(@PathVariable userId: Long): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.getCategoryIsDeleteFalse(userId)
        return ResponseEntity.ok(category)
    }

    @GetMapping("/{description}/search-by/{userId}")
    fun searchCategoryForUser(
        @RequestParam description: String,
        @PathVariable userId: Long
    ): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.findCategoryByDescription(description, userId)
        return ResponseEntity.ok(category)
    }

    @GetMapping("/search-admin/{description}")
    fun searchCategoryForAdmin(@RequestParam description: String): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.findCategoryByDescriptionToAdmin(description)
        return ResponseEntity.ok(category)
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun createCategory(@RequestBody categoryDto: CategoryDto): ResponseEntity<CategoryDto> {
        val newCategory = categoryService.addCategory(categoryDto)
        return ResponseEntity.ok(newCategory)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun updateCategory(@PathVariable id: Long, @RequestBody categoryDto: CategoryDto): ResponseEntity<CategoryDto> {
        val exCategory = categoryService.updateCategory(id, categoryDto)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(exCategory)
    }

    @PutMapping("/{id}/soft-delete")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun softDeleteCategory(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "true") isDelete: Boolean
    ): ResponseEntity<CategoryDto> {
        val exCategory = categoryService.softDeleteCategory(id, isDelete)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(exCategory)
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@PathVariable id: Long) {
        categoryService.deleteCategoryById(id)
    }

    @PutMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun moveCategory(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestParam(required = false, name = "newParentId") newParentId: Long?,
        @RequestParam(required = false, name = "parentId") parentId: Long?
    ): ResponseEntity<CategoryDto> {
        val effectiveParentId = parentId ?: newParentId
        val moved = categoryService.moveCategory(authentication.name, id, effectiveParentId)
        return ResponseEntity.ok(moved)
    }

    // Дочерние категории: всегда фильтруем по ролям текущего пользователя
    @GetMapping("/{id}/children")
    @PreAuthorize("isAuthenticated()")
    fun getChildCategories(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<List<CategoryDto>> {
        val children = categoryService.getChildCategoriesForUserEmail(id, authentication.name)
        return ResponseEntity.ok(children)
    }

    // Статьи в категории: доступ только при доступе к категории
    @GetMapping("/{id}/articles")
    @PreAuthorize("isAuthenticated()")
    fun getArticlesInCategory(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<List<ArticleDto>> {
        val articles = categoryService.getArticlesInCategoryForUserEmail(id, authentication.name)
        return ResponseEntity.ok(articles)
    }

    // Контент категории: обе части с ролевой фильтрацией
    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    fun getCategoryContent(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<CategoryContentDto> {
        val content = categoryService.getCategoryContentForUserEmail(id, authentication.name)
        return ResponseEntity.ok(content)
    }

    // Дерево категорий: ролево-фильтрованное дерево
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    fun getCategoryTree(authentication: Authentication): ResponseEntity<List<CategoryTreeDto>> {
        val tree = categoryService.getCategoryTreeForUser(authentication.name)
        return ResponseEntity.ok(tree)
    }

}
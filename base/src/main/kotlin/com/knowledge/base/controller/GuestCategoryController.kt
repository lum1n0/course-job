package com.knowledge.base.controller

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.service.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/guest/categories")
class GuestCategoryController(private val categoryService: CategoryService) {

    @GetMapping
    fun getGuestCategories(): ResponseEntity<List<CategoryDto>> {
        val guestCategories = categoryService.getGuestCategories()
        return ResponseEntity.ok(guestCategories)
    }
}
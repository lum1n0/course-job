package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.service.ArticleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/guest/articles")
class GuestArticleController(private val articleService: ArticleService) {

    @GetMapping("/by-category")
    fun getArticlesByCategory(@RequestParam categoryId: Long): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.getGuestArticlesByCategory(categoryId)
        return ResponseEntity.ok(articles)
    }

    @GetMapping("/search")
    fun searchArticles(@RequestParam description: String): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.searchGuestArticles(description)
        return ResponseEntity.ok(articles)
    }

    @GetMapping("/{id}")
    fun getArticleById(@PathVariable id: Long): ResponseEntity<ArticleDto> {
        val article = articleService.getGuestArticleById(id)
        return if (article != null) {
            ResponseEntity.ok(article)
        } else {
            ResponseEntity.status(403).build() // Запрещено, если статья не доступна для гостей
        }
    }
}
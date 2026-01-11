package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleVersionDto
import com.knowledge.base.dto.CompareResultDto
import com.knowledge.base.dto.VersionAuthorDto
import com.knowledge.base.service.ArticleVersionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/articles")
class ArticleVersionController(
    private val articleVersionService: ArticleVersionService
) {

    @GetMapping("/{articleId}/versions")
    fun listVersions(@PathVariable articleId: Long): ResponseEntity<List<ArticleVersionDto>> {
        return ResponseEntity.ok(articleVersionService.listVersions(articleId))
    }

    @GetMapping("/{articleId}/versions/{version}")
    fun getVersion(
        @PathVariable articleId: Long,
        @PathVariable version: Int
    ): ResponseEntity<ArticleVersionDto> {
        return ResponseEntity.ok(articleVersionService.getVersion(articleId, version))
    }

    // Сравнение всегда: выбранная версия vs текущая статья (игнорируя любой переданный 'to')
    @GetMapping("/{articleId}/compare")
    fun compareWithCurrent(
        @PathVariable articleId: Long,
        @RequestParam(name = "from", required = false) from: Int?,
        @RequestParam(name = "version", required = false) version: Int?, // альтернативное имя
        @RequestParam(name = "to", required = false) @Suppress("UNUSED_PARAMETER") to: Int? // игнорируем
    ): ResponseEntity<CompareResultDto> {
        val v = from ?: version
        ?: throw IllegalArgumentException("Не указана версия: передайте ?from= или ?version=")
        return ResponseEntity.ok(articleVersionService.compareWithCurrent(articleId, v))
    }

    @PostMapping("/{articleId}/versions/{version}/restore")
    fun restore(
        authentication: Authentication,
        @PathVariable articleId: Long,
        @PathVariable version: Int,
        @RequestParam(required = false) summary: String?
    ): ResponseEntity<ArticleVersionDto> {
        val dto = articleVersionService.restore(articleId, version, authentication.name, summary)
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/{articleId}/versions/{version}")
    fun deleteVersion(
        @PathVariable articleId: Long,
        @PathVariable version: Int
    ): ResponseEntity<Void> {
        articleVersionService.deleteVersion(articleId, version)
        return ResponseEntity.noContent().build()
    }
    @GetMapping("/{articleId}/versions/{version}/author")
    fun getVersionAuthor(
        @PathVariable articleId: Long,
        @PathVariable version: Int
    ): ResponseEntity<VersionAuthorDto> {
        return ResponseEntity.ok(articleVersionService.getVersionAuthor(articleId, version))
    }

}

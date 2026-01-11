package com.knowledge.base.controller

import com.fasterxml.jackson.databind.JsonNode
import com.knowledge.base.dto.ArticleDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.service.ArticleService
import com.knowledge.base.service.ArticleViewService
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.FileStorageService
import com.knowledge.base.service.PDFService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/articles")
class ArticleController(
    private val articleService: ArticleService,
    private val fileStorageService: FileStorageService,
    private val pdfService: PDFService,
    private val objectMapper: ObjectMapper,
    private val articleViewService: ArticleViewService,
    private val categoryService: CategoryService   // <- добавить
) {

    @PostMapping("/upload-image", consumes = ["multipart/form-data"])
    fun uploadImage(@RequestParam("image") image: MultipartFile): ResponseEntity<Map<String, String>> {
        return try {
            val imageUrl = fileStorageService.saveFile(image, "images")
            ResponseEntity.ok(mapOf("url" to imageUrl))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/all")
    fun getAllArticlePaginatedAdmin(pageable: Pageable): ResponseEntity<Page<ArticleDto>> {
        val articles = articleService.getAllArticle(pageable)
        return ResponseEntity.ok(articles)
    }

    @GetMapping("/by-category")
    fun getArticleForUserByCategory(
        @RequestParam categoryId: Long,
        authentication: Authentication
    ): ResponseEntity<List<ArticleDto>> {
        val articles = categoryService.getArticlesInCategoryForUserEmail(categoryId, authentication.name)
        return ResponseEntity.ok(articles)
    }


    @GetMapping("/admin/by-category")
    fun getAllArticlesByCategoryForAdmin(@RequestParam categoryId: Long, authentication: Authentication): ResponseEntity<List<ArticleDto>> {
        val username = authentication.name
        val articles = articleService.getAllArticlesByCategoryForAdmin(username, categoryId)
        return ResponseEntity.ok(articles)
    }

    @GetMapping("/slidebar/search")
    fun searchByUser(@RequestParam description: String): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.findArticleByTitle(description)
        return ResponseEntity.ok(articles)
    }

    @GetMapping("/admin/search")
    fun searchByAdmin(@RequestParam description: String): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.findArticleByTitleToAdmin(description)
        return ResponseEntity.ok(articles)
    }

    @PostMapping(consumes = ["multipart/form-data"])
    fun createArticle(
        authentication: Authentication,
        @RequestParam("title") title: String,
        @RequestParam("description") descriptionJson: String,
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleDto> {

        val descriptionNode = objectMapper.readTree(descriptionJson)

        val articleDto = ArticleDto(
            id = 0,
            title = title,
            description = descriptionNode,
            isDelete = false,
            categoryDto = articleService.getCategoryDtoById(categoryId), // Если не найдет, сервис должен кинуть ошибку
            videoPath = null,
            filePath = null
        )

        val savedArticleDto = articleService.addArticle(authentication.name, articleDto, videoFile, files)
        return ResponseEntity.ok(savedArticleDto)
    }

    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    fun updateArticle(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestParam("title") title: String,
        @RequestParam("description") descriptionJson: String,
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleDto> {
        return try {
            val descriptionNode = objectMapper.readTree(descriptionJson)
            val articleDto = ArticleDto(
                id = id,
                title = title,
                description = descriptionNode,
                isDelete = false,
                categoryDto = articleService.getCategoryDtoById(categoryId),
                videoPath = null,
                filePath = null
            )
            val updatedArticle = articleService.updateArticle(authentication.name, id, articleDto, videoFile, files)
            ResponseEntity.ok(updatedArticle)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @PatchMapping("/{id}/soft-delete")
    fun softDeleteArticle(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "true") isDelete: Boolean
    ): ResponseEntity<ArticleDto> {
        val updatedArticle = articleService.softDeleteArticle(authentication.name, id, isDelete)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updatedArticle)
    }

    @GetMapping("/{id}")
    fun getArticleById(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<ArticleDto> {
        return try {
            val article = articleService.getArticleForUserById(id, authentication.name)
                ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok(article)
        } catch (ex: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @GetMapping("/{id}/pdf")
    fun downloadArticlePdf(@PathVariable id: Long, authentication: Authentication?): ResponseEntity<ByteArray> {
        return try {
            val articleDto = articleService.getArticleById(id)
                ?: return ResponseEntity.notFound().build()
            val isPrivileged = authentication?.authorities?.any {
                it.authority == "ROLE_ADMIN" || it.authority == "ROLE_WRITER" || it.authority == "ROLE_MODERATOR"
            } ?: false
            if (articleDto.isDelete && !isPrivileged) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
            val articleEntity = articleService.getArticleEntityById(id)
                ?: return ResponseEntity.notFound().build()
            val pdfBytes = pdfService.generateArticlePdf(articleEntity)
            val fileName = URLEncoder.encode("${articleDto.title}.pdf", StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_PDF
                setContentDispositionFormData("attachment", fileName)
                contentLength = pdfBytes.size.toLong()
            }
            ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteArticle(authentication: Authentication, @PathVariable id: Long) {
        articleService.deleteArticleById(authentication.name, id)
    }
    @GetMapping("/{id}/views")
    fun getArticleViews(@PathVariable id: Long): ResponseEntity<Map<String, Long>> {
        val total = articleViewService.getTotalViews(id)
        val last24h = articleViewService.getViewsLast24h(id)
        return ResponseEntity.ok(mapOf("total" to total, "last24h" to last24h))
    }

    @PostMapping("/admin/fix-image-urls")
    @PreAuthorize("hasRole('ADMIN')")
    fun fixImageUrls(): ResponseEntity<String> {
        return try {
            val result = articleService.fixImageUrls()
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при исправлении URL: ${e.message}")
        }
    }
}

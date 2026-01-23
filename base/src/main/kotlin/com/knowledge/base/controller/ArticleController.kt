package com.knowledge.base.controller

import com.fasterxml.jackson.databind.JsonNode
import com.knowledge.base.dto.ArticleDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.knowledge.base.service.ArticleService
import com.knowledge.base.service.ArticleViewService
import com.knowledge.base.service.CategoryService
import com.knowledge.base.service.FileStorageService
import com.knowledge.base.service.PDFService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Articles", description = "Операции со статьями")
class ArticleController(
    private val articleService: ArticleService,
    private val fileStorageService: FileStorageService,
    private val pdfService: PDFService,
    private val objectMapper: ObjectMapper,
    private val articleViewService: ArticleViewService,
    private val categoryService: CategoryService
) {

    @Operation(
        summary = "Загрузка изображения",
        description = "Загружает изображение и возвращает URL для доступа к нему",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Изображение успешно загружено",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Ошибка при загрузке изображения")
        ]
    )
    @PostMapping("/upload-image", consumes = ["multipart/form-data"])
    fun uploadImage(@Parameter(description = "Файл изображения для загрузки", required = true) @RequestParam("image") image: MultipartFile): ResponseEntity<Map<String, String>> {
        return try {
            val imageUrl = fileStorageService.saveFile(image, "images")
            ResponseEntity.ok(mapOf("url" to imageUrl))
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(
        summary = "Получить все статьи (админ)",
        description = "Возвращает все статьи с пагинацией для администратора",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список статей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            )
        ]
    )
    @GetMapping("/all")
    fun getAllArticlePaginatedAdmin(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<ArticleDto>> {
        val articles = articleService.getAllArticle(pageable)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Получить статьи по категории (для пользователя)",
        description = "Возвращает список статей в указанной категории для авторизованного пользователя",
        parameters = [
            Parameter(name = "categoryId", description = "ID категории", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список статей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/by-category")
    fun getArticleForUserByCategory(
        @Parameter(description = "ID категории", required = true) @RequestParam categoryId: Long,
        authentication: Authentication
    ): ResponseEntity<List<ArticleDto>> {
        val articles = categoryService.getArticlesInCategoryForUserEmail(categoryId, authentication.name)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Получить все статьи по категории (для администратора)",
        description = "Возвращает список всех статей в указанной категории для администратора",
        parameters = [
            Parameter(name = "categoryId", description = "ID категории", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список статей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/admin/by-category")
    fun getAllArticlesByCategoryForAdmin(@Parameter(description = "ID категории", required = true) @RequestParam categoryId: Long, authentication: Authentication): ResponseEntity<List<ArticleDto>> {
        val username = authentication.name
        val articles = articleService.getAllArticlesByCategoryForAdmin(username, categoryId)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Поиск статей (для пользователя)",
        description = "Поиск статей по названию для пользователя",
        parameters = [
            Parameter(name = "description", description = "Поисковый запрос", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Результаты поиска успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/slidebar/search")
    fun searchByUser(@Parameter(description = "Поисковый запрос", required = true) @RequestParam description: String): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.findArticleByTitle(description)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Поиск статей (для администратора)",
        description = "Поиск статей по названию для администратора",
        parameters = [
            Parameter(name = "description", description = "Поисковый запрос", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Результаты поиска успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/admin/search")
    fun searchByAdmin(@Parameter(description = "Поисковый запрос", required = true) @RequestParam description: String): ResponseEntity<List<ArticleDto>> {
        val articles = articleService.findArticleByTitleToAdmin(description)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Создать новую статью",
        description = "Создает новую статью с указанными параметрами",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для создания статьи",
            required = true,
            content = [Content(
                mediaType = "multipart/form-data",
                schema = Schema(implementation = ArticleDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно создана",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные для создания статьи")
        ]
    )
    @PostMapping(consumes = ["multipart/form-data"])
    fun createArticle(
        authentication: Authentication,
        @Parameter(description = "Название статьи", required = true) @RequestParam("title") title: String,
        @Parameter(description = "Описание статьи в формате JSON", required = true) @RequestParam("description") descriptionJson: String,
        @Parameter(description = "ID категории", required = true) @RequestParam("categoryId") categoryId: Long,
        @Parameter(description = "Видео файл", required = false) @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @Parameter(description = "Дополнительные файлы", required = false) @RequestParam("files", required = false) files: List<MultipartFile>?
    ): ResponseEntity<ArticleDto> {
        return try {
            val descriptionNode = objectMapper.readTree(descriptionJson)
            val articleDto = ArticleDto(
                id = 0,
                title = title,
                description = descriptionNode,
                isDelete = false,
                categoryDto = articleService.getCategoryDtoById(categoryId),
                videoPath = null,
                filePath = null
            )

            val savedArticleDto = articleService.addArticle(authentication.name, articleDto, videoFile, files)
            ResponseEntity.ok(savedArticleDto)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(
        summary = "Обновить статью",
        description = "Обновляет существующую статью с указанными параметрами",
        parameters = [
            Parameter(name = "id", description = "ID статьи для обновления", required = true, `in` = ParameterIn.PATH)
        ],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для обновления статьи",
            required = true,
            content = [Content(
                mediaType = "multipart/form-data",
                schema = Schema(implementation = ArticleDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно обновлена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные для обновления статьи"),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    fun updateArticle(
        authentication: Authentication,
        @Parameter(description = "ID статьи для обновления", required = true) @PathVariable id: Long,
        @Parameter(description = "Новое название статьи", required = true) @RequestParam("title") title: String,
        @Parameter(description = "Новое описание статьи в формате JSON", required = true) @RequestParam("description") descriptionJson: String,
        @Parameter(description = "Новый ID категории", required = true) @RequestParam("categoryId") categoryId: Long,
        @Parameter(description = "Новый видео файл", required = false) @RequestParam("videoFile", required = false) videoFile: MultipartFile?,
        @Parameter(description = "Новые дополнительные файлы", required = false) @RequestParam("files", required = false) files: List<MultipartFile>?,
        @Parameter(description = "Список файлов для удаления", required = false) @RequestParam("removeFiles", required = false) removeFiles: List<String>?,
        @Parameter(description = "Удалить видео", required = false) @RequestParam("removeVideo", required = false) removeVideo: Boolean?
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

            val updatedArticle = articleService.updateArticle(authentication.name, id, articleDto, videoFile, files, removeFiles ?: emptyList(), removeVideo ?: false)
            ResponseEntity.ok(updatedArticle)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(
        summary = "Мягкое удаление статьи",
        description = "Помечает статью как удаленную без фактического удаления из базы данных",
        parameters = [
            Parameter(name = "id", description = "ID статьи для мягкого удаления", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "isDelete", description = "Флаг удаления (true - удалить, false - восстановить)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно помечена как удаленная",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @PatchMapping("/{id}/soft-delete")
    fun softDeleteArticle(
        authentication: Authentication,
        @Parameter(description = "ID статьи для мягкого удаления", required = true) @PathVariable id: Long,
        @Parameter(description = "Флаг удаления (true - удалить, false - восстановить)", required = false) @RequestParam(required = false, defaultValue = "true") isDelete: Boolean
    ): ResponseEntity<ArticleDto> {
        val updatedArticle = articleService.softDeleteArticle(authentication.name, id, isDelete)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updatedArticle)
    }

    @Operation(
        summary = "Получить статью по ID",
        description = "Возвращает статью по указанному ID для авторизованного пользователя",
        parameters = [
            Parameter(name = "id", description = "ID статьи", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статья успешно найдена и возвращена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleDto::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @GetMapping("/{id}")
    fun getArticleById(
        @Parameter(description = "ID статьи", required = true) @PathVariable id: Long,
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

    @Operation(
        summary = "Скачать статью в формате PDF",
        description = "Генерирует и возвращает статью в формате PDF",
        parameters = [
            Parameter(name = "id", description = "ID статьи для скачивания в формате PDF", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "PDF файл статьи успешно сгенерирован и возвращен",
                content = [Content(
                    mediaType = "application/pdf",
                    schema = Schema(type = "string", format = "binary")
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Статья не найдена"),
            ApiResponse(responseCode = "500", description = "Ошибка сервера при генерации PDF")
        ]
    )
    @GetMapping("/{id}/pdf")
    fun downloadArticlePdf(@Parameter(description = "ID статьи для скачивания в формате PDF", required = true) @PathVariable id: Long, authentication: Authentication?): ResponseEntity<ByteArray> {
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

    @Operation(
        summary = "Удалить статью",
        description = "Фактически удаляет статью из базы данных",
        parameters = [
            Parameter(name = "id", description = "ID статьи для удаления", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "200", description = "Статья успешно удалена"),
            ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @DeleteMapping("/delete/{id}")
    fun deleteArticle(authentication: Authentication, @Parameter(description = "ID статьи для удаления", required = true) @PathVariable id: Long) {
        articleService.deleteArticleById(authentication.name, id)
    }

    @Operation(
        summary = "Получить количество просмотров статьи",
        description = "Возвращает общее количество просмотров и количество просмотров за последние 24 часа для статьи",
        parameters = [
            Parameter(name = "id", description = "ID статьи", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Данные о просмотрах успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Map::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Статья не найдена")
        ]
    )
    @GetMapping("/{id}/views")
    fun getArticleViews(@Parameter(description = "ID статьи", required = true) @PathVariable id: Long): ResponseEntity<Map<String, Long>> {
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

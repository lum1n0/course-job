package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleDto
import com.knowledge.base.dto.CategoryContentDto
import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.CategoryTreeDto
import com.knowledge.base.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/category")
@Tag(name = "Categories", description = "Управление категориями")
class CategoryController(private val categoryService: CategoryService) {

    @Operation(
        summary = "Получить все категории (админ)",
        description = "Возвращает все категории с пагинацией для администратора",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список категорий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Page::class)
                )]
            )
        ]
    )
    @GetMapping("/all")
    fun getAllCategoryForAdmin(@Parameter(hidden = true) pageable: Pageable): ResponseEntity<Page<CategoryDto>> {
        val category = categoryService.getAllCategory(pageable)
        return ResponseEntity.ok(category)
    }

    @Operation(
        summary = "Получить категории для пользователя",
        description = "Возвращает категории, доступные для указанного пользователя",
        parameters = [
            Parameter(name = "userId", description = "ID пользователя", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список категорий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/{userId}/for-user-all")
    @PreAuthorize("isAuthenticated()")
    fun getAllCategoryForUsers(@Parameter(description = "ID пользователя", required = true) @PathVariable userId: Long): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.getCategoryIsDeleteFalse(userId)
        return ResponseEntity.ok(category)
    }

    @Operation(
        summary = "Поиск категорий для пользователя",
        description = "Поиск категорий по описанию для указанного пользователя",
        parameters = [
            Parameter(name = "description", description = "Поисковый запрос", required = true, `in` = ParameterIn.QUERY),
            Parameter(name = "userId", description = "ID пользователя", required = true, `in` = ParameterIn.PATH)
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
    @GetMapping("/{description}/search-by/{userId}")
    fun searchCategoryForUser(
        @Parameter(description = "Поисковый запрос", required = true) @RequestParam description: String,
        @Parameter(description = "ID пользователя", required = true) @PathVariable userId: Long
    ): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.findCategoryByDescription(description, userId)
        return ResponseEntity.ok(category)
    }

    @Operation(
        summary = "Поиск категорий (админ)",
        description = "Поиск категорий по описанию для администратора",
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
    @GetMapping("/search-admin/{description}")
    fun searchCategoryForAdmin(@Parameter(description = "Поисковый запрос", required = true) @RequestParam description: String): ResponseEntity<List<CategoryDto>> {
        val category = categoryService.findCategoryByDescriptionToAdmin(description)
        return ResponseEntity.ok(category)
    }

    @Operation(
        summary = "Создать категорию",
        description = "Создает новую категорию",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для создания категории",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CategoryDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Категория успешно создана",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CategoryDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверные данные")
        ]
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun createCategory(@RequestBody categoryDto: CategoryDto): ResponseEntity<CategoryDto> {
        val newCategory = categoryService.addCategory(categoryDto)
        return ResponseEntity.ok(newCategory)
    }

    @Operation(
        summary = "Обновить категорию",
        description = "Обновляет существующую категорию",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH)
        ],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для обновления категории",
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CategoryDto::class)
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Категория успешно обновлена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CategoryDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Категория не найдена")
        ]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun updateCategory(
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long,
        @RequestBody categoryDto: CategoryDto
    ): ResponseEntity<CategoryDto> {
        val exCategory = categoryService.updateCategory(id, categoryDto)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(exCategory)
    }

    @Operation(
        summary = "Мягкое удаление категории",
        description = "Помечает категорию как удаленную (мягкое удаление)",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "isDelete", description = "Флаг удаления", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Категория успешно помечена как удаленная",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CategoryDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Категория не найдена")
        ]
    )
    @PutMapping("/{id}/soft-delete")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun softDeleteCategory(
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long,
        @Parameter(description = "Флаг удаления", required = false) @RequestParam(required = false, defaultValue = "true") isDelete: Boolean
    ): ResponseEntity<CategoryDto> {
        val exCategory = categoryService.softDeleteCategory(id, isDelete)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(exCategory)
    }

    @Operation(
        summary = "Удалить категорию",
        description = "Полностью удаляет категорию из системы",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Категория успешно удалена"),
            ApiResponse(responseCode = "404", description = "Категория не найдена")
        ]
    )
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@Parameter(description = "ID категории", required = true) @PathVariable id: Long) {
        categoryService.deleteCategoryById(id)
    }

    @Operation(
        summary = "Переместить категорию",
        description = "Перемещает категорию в другую родительскую категорию",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "newParentId", description = "ID новой родительской категории", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "parentId", description = "ID родительской категории (альтернатива)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Категория успешно перемещена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CategoryDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Категория не найдена")
        ]
    )
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','WRITER')")
    fun moveCategory(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long,
        @Parameter(description = "ID новой родительской категории", required = false) @RequestParam(required = false, name = "newParentId") newParentId: Long?,
        @Parameter(description = "ID родительской категории (альтернатива)", required = false) @RequestParam(required = false, name = "parentId") parentId: Long?
    ): ResponseEntity<CategoryDto> {
        val effectiveParentId = parentId ?: newParentId
        val moved = categoryService.moveCategory(authentication.name, id, effectiveParentId)
        return ResponseEntity.ok(moved)
    }

    @Operation(
        summary = "Получить дочерние категории",
        description = "Возвращает список дочерних категорий для указанной категории",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список дочерних категорий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/{id}/children")
    @PreAuthorize("isAuthenticated()")
    fun getChildCategories(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long
    ): ResponseEntity<List<CategoryDto>> {
        val children = categoryService.getChildCategoriesForUserEmail(id, authentication.name)
        return ResponseEntity.ok(children)
    }

    @Operation(
        summary = "Получить статьи в категории",
        description = "Возвращает список статей в указанной категории",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH)
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
    @GetMapping("/{id}/articles")
    @PreAuthorize("isAuthenticated()")
    fun getArticlesInCategory(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long
    ): ResponseEntity<List<ArticleDto>> {
        val articles = categoryService.getArticlesInCategoryForUserEmail(id, authentication.name)
        return ResponseEntity.ok(articles)
    }

    @Operation(
        summary = "Получить контент категории",
        description = "Возвращает контент категории (дочерние категории и статьи)",
        parameters = [
            Parameter(name = "id", description = "ID категории", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Контент категории успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CategoryContentDto::class)
                )]
            )
        ]
    )
    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    fun getCategoryContent(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID категории", required = true) @PathVariable id: Long
    ): ResponseEntity<CategoryContentDto> {
        val content = categoryService.getCategoryContentForUserEmail(id, authentication.name)
        return ResponseEntity.ok(content)
    }

    @Operation(
        summary = "Получить дерево категорий",
        description = "Возвращает иерархическое дерево категорий для текущего пользователя",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Дерево категорий успешно получено",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    fun getCategoryTree(@Parameter(hidden = true) authentication: Authentication): ResponseEntity<List<CategoryTreeDto>> {
        val tree = categoryService.getCategoryTreeForUser(authentication.name)
        return ResponseEntity.ok(tree)
    }
}

package com.knowledge.base.controller

import com.knowledge.base.dto.ArticleVersionDto
import com.knowledge.base.dto.CompareResultDto
import com.knowledge.base.dto.VersionAuthorDto
import com.knowledge.base.service.ArticleVersionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Article Versions", description = "Управление версиями статей")
class ArticleVersionController(
    private val articleVersionService: ArticleVersionService
) {

    @Operation(
        summary = "Получить список версий статьи",
        description = "Возвращает все версии указанной статьи",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список версий успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            )
        ]
    )
    @GetMapping("/{articleId}/versions")
    fun listVersions(@Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long): ResponseEntity<List<ArticleVersionDto>> {
        return ResponseEntity.ok(articleVersionService.listVersions(articleId))
    }

    @Operation(
        summary = "Получить конкретную версию статьи",
        description = "Возвращает указанную версию статьи",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "version", description = "Номер версии", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Версия успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleVersionDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Версия не найдена")
        ]
    )
    @GetMapping("/{articleId}/versions/{version}")
    fun getVersion(
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Номер версии", required = true) @PathVariable version: Int
    ): ResponseEntity<ArticleVersionDto> {
        return ResponseEntity.ok(articleVersionService.getVersion(articleId, version))
    }

    @Operation(
        summary = "Сравнить версию с текущей статьей",
        description = "Сравнивает выбранную версию с текущей версией статьи",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "from", description = "Номер версии для сравнения", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "version", description = "Номер версии (альтернатива)", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "to", description = "Игнорируется (всегда текущая версия)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Результат сравнения получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CompareResultDto::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Не указана версия для сравнения")
        ]
    )
    @GetMapping("/{articleId}/compare")
    fun compareWithCurrent(
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Номер версии для сравнения", required = false) @RequestParam(name = "from", required = false) from: Int?,
        @Parameter(description = "Номер версии (альтернатива)", required = false) @RequestParam(name = "version", required = false) version: Int?,
        @Parameter(description = "Игнорируется", required = false) @RequestParam(name = "to", required = false) @Suppress("UNUSED_PARAMETER") to: Int?
    ): ResponseEntity<CompareResultDto> {
        val v = from ?: version
        ?: throw IllegalArgumentException("Не указана версия: передайте ?from= или ?version=")
        return ResponseEntity.ok(articleVersionService.compareWithCurrent(articleId, v))
    }

    @Operation(
        summary = "Восстановить версию статьи",
        description = "Восстанавливает указанную версию статьи как текущую",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "version", description = "Номер версии", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "summary", description = "Комментарий к восстановлению", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Версия успешно восстановлена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ArticleVersionDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Версия не найдена")
        ]
    )
    @PostMapping("/{articleId}/versions/{version}/restore")
    fun restore(
        @Parameter(hidden = true) authentication: Authentication,
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Номер версии", required = true) @PathVariable version: Int,
        @Parameter(description = "Комментарий к восстановлению", required = false) @RequestParam(required = false) summary: String?
    ): ResponseEntity<ArticleVersionDto> {
        val dto = articleVersionService.restore(articleId, version, authentication.name, summary)
        return ResponseEntity.ok(dto)
    }

    @Operation(
        summary = "Удалить версию статьи",
        description = "Удаляет указанную версию статьи",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "version", description = "Номер версии", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(responseCode = "204", description = "Версия успешно удалена"),
            ApiResponse(responseCode = "404", description = "Версия не найдена")
        ]
    )
    @DeleteMapping("/{articleId}/versions/{version}")
    fun deleteVersion(
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Номер версии", required = true) @PathVariable version: Int
    ): ResponseEntity<Void> {
        articleVersionService.deleteVersion(articleId, version)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Получить автора версии",
        description = "Возвращает информацию об авторе указанной версии статьи",
        parameters = [
            Parameter(name = "articleId", description = "ID статьи", required = true, `in` = ParameterIn.PATH),
            Parameter(name = "version", description = "Номер версии", required = true, `in` = ParameterIn.PATH)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Информация об авторе получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = VersionAuthorDto::class)
                )]
            ),
            ApiResponse(responseCode = "404", description = "Версия не найдена")
        ]
    )
    @GetMapping("/{articleId}/versions/{version}/author")
    fun getVersionAuthor(
        @Parameter(description = "ID статьи", required = true) @PathVariable articleId: Long,
        @Parameter(description = "Номер версии", required = true) @PathVariable version: Int
    ): ResponseEntity<VersionAuthorDto> {
        return ResponseEntity.ok(articleVersionService.getVersionAuthor(articleId, version))
    }
}

package com.knowledge.base.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable

@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "Управление файлами")
class FileController {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    @Operation(
        summary = "Потоковая передача видео",
        description = "Возвращает видео файл для потоковой передачи",
        parameters = [
            Parameter(name = "path", description = "Путь к видео файлу", required = true, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Видео файл успешно получен",
                content = [Content(
                    mediaType = "video/*",
                    schema = Schema(type = "string", format = "binary")
                )]
            ),
            ApiResponse(responseCode = "400", description = "Неверный путь к файлу"),
            ApiResponse(responseCode = "404", description = "Файл не найден")
        ]
    )
    @GetMapping("/video/stream")
    fun streamVideo(@Parameter(description = "Путь к видео файлу", required = true) @RequestParam path: String): ResponseEntity<Resource> {
        val decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString())
        val cleanPath = decodedPath.removePrefix("/")
        val filePath: Path = Paths.get(uploadDir).resolve(cleanPath).normalize()

        println("Запрос файла: $path")
        println("Ищем по пути: ${filePath.toAbsolutePath()}")

        if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
            return ResponseEntity.badRequest().build()
        }

        if (!filePath.exists() || !filePath.isReadable()) {
            println("Файл не найден: ${filePath.toAbsolutePath()}")
            return ResponseEntity.notFound().build()
        }

        val resource = UrlResource(filePath.toUri())
        val contentType = MediaTypeFactory.getMediaType(resource)
            .orElse(MediaType.APPLICATION_OCTET_STREAM)

        return ResponseEntity.ok()
            .contentType(contentType)
            .body(resource)
    }
}

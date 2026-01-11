package com.knowledge.base.controller

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
class FileController {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    @GetMapping("/video/stream")
    fun streamVideo(@RequestParam path: String): ResponseEntity<Resource> {
        // БЫЛО: val cleanPath = path.removePrefix("/videos/").removePrefix("/")
        val decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString())


        // СТАЛО: Удаляем только начальный слэш, сохраняя структуру папок (videos/...)
        val cleanPath = decodedPath.removePrefix("/")

        val filePath: Path = Paths.get(uploadDir).resolve(cleanPath).normalize()

        // Логирование для отладки (можно убрать потом)
        println("Запрос файла: $path")
        println("Ищем по пути: ${filePath.toAbsolutePath()}")

        if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
            return ResponseEntity.badRequest().build()
        }

        if (!filePath.exists() || !filePath.isReadable()) {
            // Добавьте лог, чтобы видеть в консоли сервера, где именно не найден файл
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

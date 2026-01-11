package com.knowledge.base.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import jakarta.annotation.PostConstruct

@Service
class FileStorageService {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    private val allowedVideoTypes = setOf("video/mp4", "video/avi", "video/mov", "video/wmv")
    private val allowedImageTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg")
    private val allowedDocumentTypes = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.oasis.opendocument.presentation",
        "application/vnd.oasis.opendocument.spreadsheet",
        "application/vnd.oasis.opendocument.text",
        "text/csv",
        "application/rtf",
        "text/rtf"
    )

    private val allowedImageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")
    private val allowedVideoExtensions = setOf("mp4", "avi", "mov", "wmv")
    private val allowedDocumentExtensions = setOf(
        "pdf", "doc", "docx", "txt", "xls", "xlsx",
        "ppt", "pptx", "odp", "ods", "odt", "csv", "rtf"
    )

    @PostConstruct
    private fun init() {
        createDirectories()
    }

    private fun createDirectories() {
        try {
            val baseDir = Paths.get(uploadDir)
            Files.createDirectories(baseDir)
            Files.createDirectories(baseDir.resolve("videos"))
            Files.createDirectories(baseDir.resolve("files"))
            Files.createDirectories(baseDir.resolve("images"))
        } catch (e: IOException) {
            throw RuntimeException("Не удалось создать директории для загрузки файлов", e)
        }
    }

    fun saveFile(file: MultipartFile, type: String): String {
        validateFile(file, type)
        val fileName = generateFileName(file.originalFilename ?: "unknown", type)
        val dirPath = Paths.get(uploadDir, type)
        val filePath = dirPath.resolve(fileName)
        try {
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw RuntimeException("Ошибка при сохранении файла: ${e.message}", e)
        }

        return "/$type/$fileName"
    }

    private fun validateFile(file: MultipartFile, type: String) {
        if (file.isEmpty) {
            throw IllegalArgumentException("Файл не может быть пустым")
        }

        val contentType = file.contentType?.lowercase()
        val extension = file.originalFilename?.substringAfterLast('.', "")?.lowercase()

        val isTypeValid = when (type) {
            "images" -> contentType in allowedImageTypes
            "videos" -> contentType in allowedVideoTypes
            "files" -> contentType in allowedDocumentTypes
            else -> false
        }

        val isExtensionValid = when (type) {
            "images" -> extension in allowedImageExtensions
            "videos" -> extension in allowedVideoExtensions
            "files" -> extension in allowedDocumentExtensions
            else -> false
        }

        if (!isTypeValid && !isExtensionValid) {
            throw IllegalArgumentException("Недопустимый тип файла. Тип: '$contentType', Расширение: '$extension'")
        }

        when (type) {
            "images" -> if (file.size > 10 * 1024 * 1024) throw IllegalArgumentException("Размер изображения не должен превышать 10MB")
            "videos" -> if (file.size > 100 * 1024 * 1024) throw IllegalArgumentException("Размер видео не должен превышать 100MB")
            "files" -> if (file.size > 50 * 1024 * 1024) throw IllegalArgumentException("Размер документа не должен превышать 50MB")
        }
    }

    private fun generateFileName(originalFilename: String, type: String): String {
        val extension = originalFilename.substringAfterLast('.', "")
        val nameWithoutExtension = originalFilename.substringBeforeLast('.')

        // Основное исправление: разрешаем кириллицу и сохраняем оригинальные символы
        val sanitizedName = nameWithoutExtension.replace(Regex("""[^\p{L}\p{Nd}._-]"""), "_")

        if (type == "files") {
            return if (extension.isNotEmpty()) "$sanitizedName.$extension" else sanitizedName
        }
        return if (extension.isNotEmpty()) {
            "${UUID.randomUUID()}_${sanitizedName}.$extension"
        } else {
            "${UUID.randomUUID()}_$sanitizedName"
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val fullPath = Paths.get(uploadDir, filePath.removePrefix("/"))
            Files.deleteIfExists(fullPath)
        } catch (e: IOException) {
            false
        }
    }
}
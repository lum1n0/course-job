package com.knowledge.base.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig : WebMvcConfigurer {

    // Внедряем путь к папке для загрузок из application-dev.properties
    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Формируем путь к директории с файлами.
        // "file:" - это обязательный префикс для указания на файловую систему.
        // toAbsolutePath() преобразует относительный путь (например, ./uploads) в полный.
        val resourcePath = "file:${java.io.File(uploadDir).absolutePath}/"

        // Регистрируем обработчик для видео
        // Все запросы по URL /videos/** будут искать файлы в папке uploads/videos/
        registry.addResourceHandler("/videos/**")
            .addResourceLocations("${resourcePath}videos/")

        // Регистрируем обработчик для других файлов
        // Все запросы по URL /files/** будут искать файлы в папке uploads/files/
        registry.addResourceHandler("/files/**")
            .addResourceLocations("${resourcePath}files/")

        // Регистрируем обработчик для изображений
        // Все запросы по URL /images/** будут искать файлы в папке uploads/images/
        registry.addResourceHandler("/images/**")
            .addResourceLocations("${resourcePath}images/")
    }
}

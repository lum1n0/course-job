package com.knowledge.base.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/swagger-ui")
class SwaggerCustomController {

    @GetMapping("/swagger-ui.css", produces = ["text/css"])
    fun getCustomCss(): String {
        // Загружаем оригинальный CSS из webjar
        val webjarCss = ClassPathResource("/META-INF/resources/webjars/swagger-ui/5.10.3/swagger-ui.css")
        val originalCss = StreamUtils.copyToString(webjarCss.inputStream, StandardCharsets.UTF_8)

        // Загружаем кастомный CSS
        val customCss = ClassPathResource("/static/swagger-ui/custom.css")
        val customStyles = StreamUtils.copyToString(customCss.inputStream, StandardCharsets.UTF_8)

        // Объединяем
        return originalCss + "\n\n/* Custom Styles */\n" + customStyles
    }

    @GetMapping("/custom.js", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCustomJs(): String {
        val customJs = ClassPathResource("/static/swagger-ui/custom.js")
        return StreamUtils.copyToString(customJs.inputStream, StandardCharsets.UTF_8)
    }
}

// src/main/kotlin/com/knowledge/base/config/WebMvcViewConfig.kt
package com.knowledge.base.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcViewConfig(
    private val articleViewInterceptor: ArticleViewInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(articleViewInterceptor)
            .addPathPatterns("/api/articles/*")
    }
}

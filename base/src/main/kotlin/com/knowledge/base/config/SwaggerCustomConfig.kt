package com.knowledge.base.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SwaggerCustomConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/swagger-ui/**")
            .addResourceLocations(
                "classpath:/static/swagger-ui/",
                "classpath:/META-INF/resources/webjars/swagger-ui/5.10.3/",
                "classpath:/META-INF/resources/"
            )
            .resourceChain(false)
    }
}

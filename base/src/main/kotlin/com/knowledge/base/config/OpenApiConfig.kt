package com.knowledge.base.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeNames = listOf("bearerAuth")

        return OpenAPI()
            .info(
                Info()
                    .title("Pro Znania API")
                    .version("1.0.0")
                    .description("Документация API для образовательной платформы")
                    .contact(
                        Contact()
                            .name("Команда разработчиков Pro Znania")
                            .email("support@proznania.ru")
                            .url("https://proznania.ru/contact")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
            .tags(
                mutableListOf(
//                    Tag().name("Authentication").description("Операции аутентификации"),
//                    Tag().name("Articles").description("Операции со статьями"),
//                    Tag().name("Users").description("Операции с пользователями"),
//                    Tag().name("Knowledge Base").description("Операции с базой знаний"),
//                    Tag().name("AI Services").description("Операции с ИИ сервисами")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Authorization header using the Bearer scheme")
                    )
                    .addResponses(
                        "Unauthorized",
                        ApiResponse()
                            .description("Unauthorized - Invalid or expired token")
                            .content(Content())
                    )
                    .addResponses(
                        "Forbidden",
                        ApiResponse()
                            .description("Forbidden - Insufficient permissions")
                            .content(Content())
                    )
            )
    }
}

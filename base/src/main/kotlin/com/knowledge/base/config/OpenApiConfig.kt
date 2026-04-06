package com.knowledge.base.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Pageable

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
                    .description(
                        "Документация API для образовательной платформы.\n\n" +
                            "Полезные ссылки:\n" +
                            "- [Демо чата](/demo/chat.html)\n" +
                            "- [Дашборды](/demo/dashboard.html)"
                    )
                    .contact(
                        Contact()
                            .name("Команда разработчиков Pro Znania")
                            .email("support@proznania.ru")
                            .url("#")
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

    /**
     * Кастомайзер для отображения параметров пагинации в Swagger UI.
     * Заменяет скрытый параметр Pageable на отдельные параметры page, size, sort.
     */
    @Bean
    fun pageableOperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        val hasPageable = handlerMethod.methodParameters.any { param ->
            Pageable::class.java.isAssignableFrom(param.parameterType)
        }

        if (hasPageable) {
            // Удаляем скрытый параметр pageable если он есть
            operation.parameters?.removeIf { it.name == "pageable" }

            // Добавляем явные параметры пагинации
            operation.addParametersItem(
                Parameter()
                    .name("page")
                    .`in`("query")
                    .description("Номер страницы (начиная с 0)")
                    .required(false)
                    .schema(IntegerSchema().example(0))
            )
            operation.addParametersItem(
                Parameter()
                    .name("size")
                    .`in`("query")
                    .description("Количество элементов на странице")
                    .required(false)
                    .schema(IntegerSchema().example(20))
            )
            operation.addParametersItem(
                Parameter()
                    .name("sort")
                    .`in`("query")
                    .description("Сортировка (формат: field,asc|desc). Пример: id,desc")
                    .required(false)
                    .schema(StringSchema().example("id,desc"))
            )
        }
        operation
    }

    @Bean
    fun multipartFileOperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, _ ->
        val multipartSchema = operation.requestBody
            ?.content
            ?.get("multipart/form-data")
            ?.schema

        multipartSchema?.properties?.let { properties ->
            if (properties.containsKey("image")) {
                properties["image"] = StringSchema().format("binary")
            }

            if (properties.containsKey("videoFile")) {
                properties["videoFile"] = StringSchema().format("binary")
            }

            if (properties.containsKey("files")) {
                properties["files"] = ArraySchema().items(StringSchema().format("binary"))
            }
        }

        operation
    }
}

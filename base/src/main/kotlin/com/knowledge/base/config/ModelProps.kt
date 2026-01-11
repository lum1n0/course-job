// ModelProps.kt
package com.knowledge.base.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

data class ModelNames(
    val chatModel: String,
    val embedModel: String
)

@Configuration
@Profile("dev")
class DevModelsConfig {
    @Bean
    fun devModels() = ModelNames(
        chatModel = "llama3",
        embedModel = "nomic-embed-text"
    )
}

@Configuration
@Profile("prod")
class ProdModelsConfig {
    @Bean
    fun prodModels() = ModelNames(
        chatModel = "qwen2.5:14b-instruct-q4_K_M",
        embedModel = "bge-m3"
    )
}
package com.knowledge.base.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    // --- ДОБАВЛЕННЫЙ МЕТОД: Решает проблему с предупреждением ---
    @Bean
    fun taskExecutor(): TaskExecutor {
        // Создаем и возвращаем простой исполнитель задач,
        // который Spring будет использовать для @Async по умолчанию.
        return SimpleAsyncTaskExecutor()
    }
}

package com.knowledge.base

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.DisplayName

/**
 * Основной тестовый класс приложения.
 * Запускает базовую проверку загрузки контекста Spring.
 * Для запуска всех тестов используйте команду: ./gradlew test
 */
@SpringBootTest
class BaseApplicationTests {

    @Test
    @DisplayName("Проверка загрузки контекста Spring")
    fun contextLoads() {
        println("✓ Контекст Spring успешно загружен")
    }
}

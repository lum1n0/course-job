package com.knowledge.base.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

object PasswordHasherUtil {
    @JvmStatic
    fun main(args: Array<String>) {
        // 1. Укажите здесь пароль для хеширования
        val rawPassword = "Toor123!"

        // 2. Создаем кодировщик (точно такой же, как в Spring Security)
        val passwordEncoder = BCryptPasswordEncoder()

        // 3. Хешируем пароль
        val hashedPassword = passwordEncoder.encode(rawPassword)

        // 4. Выводим результаты
        println("==============================================")
        println("Original Password: $rawPassword")
        println("Hashed Password  : $hashedPassword")
        println("==============================================")

        // 5. Опциональная проверка корректности хеширования
        val isMatch = passwordEncoder.matches(rawPassword, hashedPassword)
        println("Password Verification: $isMatch")
    }
}
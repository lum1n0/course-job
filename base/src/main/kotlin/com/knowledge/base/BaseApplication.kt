package com.knowledge.base

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling // <-- Добавьте эту строку
@EnableAsync // <-- Добавьте эту аннотацию
class BaseApplication

fun main(args: Array<String>) {
	runApplication<BaseApplication>(*args)
}
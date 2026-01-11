// src/main/kotlin/com/knowledge/base/controller/StatisticsController.kt
package com.knowledge.base.controller

import com.knowledge.base.dto.CountersDtoArticles
import com.knowledge.base.dto.CountersDtoCategories
import com.knowledge.base.dto.FrequencyResponse
import com.knowledge.base.dto.StatPeriod
import com.knowledge.base.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/stats")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping("/counters-articles")
    fun countersArticles(): CountersDtoArticles =
        statisticsService.getCountersArticles()

    @GetMapping("/counters-categories")
    fun countersCategories(): CountersDtoCategories =
        statisticsService.getCountersCategories()

    @GetMapping("/articles/frequency")
    fun articleFrequency(
        @RequestParam(defaultValue = "DAY") period: StatPeriod,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): FrequencyResponse =
        statisticsService.getArticleFrequency(period, from, to)

    @GetMapping("/categories/frequency")
    fun categoryFrequency(
        @RequestParam(defaultValue = "DAY") period: StatPeriod,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): FrequencyResponse =
        statisticsService.getCategoryFrequency(period, from, to)
}

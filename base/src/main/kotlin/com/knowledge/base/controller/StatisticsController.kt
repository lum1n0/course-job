package com.knowledge.base.controller

import com.knowledge.base.dto.CountersDtoArticles
import com.knowledge.base.dto.CountersDtoCategories
import com.knowledge.base.dto.FrequencyResponse
import com.knowledge.base.dto.StatPeriod
import com.knowledge.base.service.StatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Статистика по статьям и категориям")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @Operation(
        summary = "Получить счетчики статей",
        description = "Возвращает общую статистику по статьям",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Счетчики успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CountersDtoArticles::class)
                )]
            )
        ]
    )
    @GetMapping("/counters-articles")
    fun countersArticles(): CountersDtoArticles =
        statisticsService.getCountersArticles()

    @Operation(
        summary = "Получить счетчики категорий",
        description = "Возвращает общую статистику по категориям",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Счетчики успешно получены",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CountersDtoCategories::class)
                )]
            )
        ]
    )
    @GetMapping("/counters-categories")
    fun countersCategories(): CountersDtoCategories =
        statisticsService.getCountersCategories()

    @Operation(
        summary = "Получить частоту создания статей",
        description = "Возвращает статистику частоты создания статей по периодам",
        parameters = [
            Parameter(name = "period", description = "Период группировки (DAY, WEEK, MONTH, YEAR)", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "from", description = "Дата начала периода (ISO формат)", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "to", description = "Дата окончания периода (ISO формат)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статистика успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FrequencyResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/articles/frequency")
    fun articleFrequency(
        @Parameter(description = "Период группировки", required = false) @RequestParam(defaultValue = "DAY") period: StatPeriod,
        @Parameter(description = "Дата начала периода", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @Parameter(description = "Дата окончания периода", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): FrequencyResponse =
        statisticsService.getArticleFrequency(period, from, to)

    @Operation(
        summary = "Получить частоту создания категорий",
        description = "Возвращает статистику частоты создания категорий по периодам",
        parameters = [
            Parameter(name = "period", description = "Период группировки (DAY, WEEK, MONTH, YEAR)", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "from", description = "Дата начала периода (ISO формат)", required = false, `in` = ParameterIn.QUERY),
            Parameter(name = "to", description = "Дата окончания периода (ISO формат)", required = false, `in` = ParameterIn.QUERY)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Статистика успешно получена",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FrequencyResponse::class)
                )]
            )
        ]
    )
    @GetMapping("/categories/frequency")
    fun categoryFrequency(
        @Parameter(description = "Период группировки", required = false) @RequestParam(defaultValue = "DAY") period: StatPeriod,
        @Parameter(description = "Дата начала периода", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @Parameter(description = "Дата окончания периода", required = false) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): FrequencyResponse =
        statisticsService.getCategoryFrequency(period, from, to)
}

// src/main/kotlin/com/knowledge/base/service/StatisticsService.kt
package com.knowledge.base.service

import com.knowledge.base.dto.CountersDtoArticles
import com.knowledge.base.dto.CountersDtoCategories
import com.knowledge.base.dto.FrequencyResponse
import com.knowledge.base.dto.SeriesPointDto
import com.knowledge.base.dto.StatPeriod
import com.knowledge.base.repository.StatisticsRepository
import org.springframework.stereotype.Service
import java.time.*

@Service
class StatisticsService(
    private val statisticsRepository: StatisticsRepository
) {

    fun getCountersArticles(): CountersDtoArticles {
        val articles = statisticsRepository.countActiveArticles()
        return CountersDtoArticles(totalArticles = articles)
    }

    fun getCountersCategories(): CountersDtoCategories {
        val categories = statisticsRepository.countActiveCategories()
        return CountersDtoCategories(totalCategories = categories)
    }

    fun getArticleFrequency(
        period: StatPeriod,
        from: LocalDate?,
        to: LocalDate?
    ): FrequencyResponse {
        val (fromInstant, toInstant) = resolveRange(from, to)
        val rows = statisticsRepository.articleCreationFrequency(
            period = period.name,
            from = fromInstant,
            to = toInstant
        )
        val points = rows.map { SeriesPointDto(bucket = it.bucket, count = it.cnt) }
        return FrequencyResponse(period = period, from = fromInstant, to = toInstant, points = points)
    }

    fun getCategoryFrequency(
        period: StatPeriod,
        from: LocalDate?,
        to: LocalDate?
    ): FrequencyResponse {
        val (fromInstant, toInstant) = resolveRange(from, to)
        val rows = statisticsRepository.categoryCreationFrequency(
            period = period.name,
            from = fromInstant,
            to = toInstant
        )
        val points = rows.map { SeriesPointDto(bucket = it.bucket, count = it.cnt) }
        return FrequencyResponse(period = period, from = fromInstant, to = toInstant, points = points)
    }

    private fun resolveRange(from: LocalDate?, to: LocalDate?): Pair<Instant, Instant> {
        val zone = ZoneOffset.UTC
        val toDate = to ?: LocalDate.now(zone)
        val fromDate = from ?: toDate.minusDays(90)
        val fromInstant = fromDate.atStartOfDay(zone).toInstant()
        val toInstant = toDate.plusDays(1).atStartOfDay(zone).toInstant() // полуинтервал [from, to)
        return fromInstant to toInstant
    }
}

// src/main/kotlin/com/knowledge/base/dto/stats/StatsDtos.kt
package com.knowledge.base.dto

import java.time.Instant

enum class StatPeriod { DAY, WEEK, MONTH }

data class CountersDtoArticles(
    val totalArticles: Long,
)

data class CountersDtoCategories(
    val totalCategories: Long
)

data class SeriesPointDto(
    val bucket: Instant,
    val count: Long
)

data class FrequencyResponse(
    val period: StatPeriod,
    val from: Instant,
    val to: Instant,
    val points: List<SeriesPointDto>
)

// src/main/kotlin/com/knowledge/base/repository/StatisticsRepository.kt
package com.knowledge.base.repository

import com.knowledge.base.model.Article
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface StatisticsRepository : JpaRepository<Article, Long> {

    interface BucketCountRow {
        val bucket: Instant
        val cnt: Long
    }

    @Query(
        value = """
        WITH first_versions AS (
            SELECT av.article_id, MIN(av.edited_at) AS created_at
            FROM article_version av
            GROUP BY av.article_id
        ),
        active_articles AS (
            SELECT a.article_id
            FROM article a
            WHERE a.is_delete = false
        )
        SELECT 
            date_trunc(
                CASE 
                    WHEN :period = 'DAY' THEN 'day'
                    WHEN :period = 'WEEK' THEN 'week'
                    ELSE 'month'
                END, 
                fv.created_at
            ) AS bucket,
            COUNT(*) AS cnt
        FROM first_versions fv
        JOIN active_articles aa ON aa.article_id = fv.article_id
        WHERE fv.created_at >= :from AND fv.created_at < :to
        GROUP BY bucket
        ORDER BY bucket
        """,
        nativeQuery = true
    )
    fun articleCreationFrequency(
        @Param("period") period: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<BucketCountRow>

    @Query(
        value = """
        SELECT 
            date_trunc(
                CASE 
                    WHEN :period = 'DAY' THEN 'day'
                    WHEN :period = 'WEEK' THEN 'week'
                    ELSE 'month'
                END, 
                c.created_at
            ) AS bucket,
            COUNT(*) AS cnt
        FROM categories c
        WHERE c.is_delete = false
          AND c.created_at IS NOT NULL
          AND c.created_at >= :from AND c.created_at < :to
        GROUP BY bucket
        ORDER BY bucket
        """,
        nativeQuery = true
    )
    fun categoryCreationFrequency(
        @Param("period") period: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<BucketCountRow>

    @Query(value = "SELECT COUNT(*) FROM article a WHERE a.is_delete = false", nativeQuery = true)
    fun countActiveArticles(): Long

    @Query(value = "SELECT COUNT(*) FROM categories c WHERE c.is_delete = false", nativeQuery = true)
    fun countActiveCategories(): Long
}

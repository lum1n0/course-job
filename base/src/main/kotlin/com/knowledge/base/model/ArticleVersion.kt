package com.knowledge.base.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import com.knowledge.base.config.StringListConverter

@Entity
@Table(
    name = "article_version",
    uniqueConstraints = [UniqueConstraint(columnNames = ["article_id", "version"])]
)
data class ArticleVersion(
    @Id
    @Column(nullable = false, name = "article_version_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "article_version_seq")
    @SequenceGenerator(name = "article_version_seq", sequenceName = "article_version_sequence", allocationSize = 1)
    val id: Long = 0,

    @Column(nullable = false, name = "article_id")
    val articleId: Long,

    @Column(nullable = false, name = "version")
    val version: Int,

    @Column(nullable = false, name = "title")
    val title: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", columnDefinition = "jsonb")
    val description: JsonNode? = null,

    @Column(nullable = false, name = "category_id")
    val categoryId: Long,

    @Convert(converter = StringListConverter::class)
    @Column(name = "video_path", columnDefinition = "text")
    val videoPath: List<String>? = null,

    @Convert(converter = StringListConverter::class)
    @Column(name = "file_path", columnDefinition = "text")
    val filePath: List<String>? = null,

    @Column(name = "edited_by_id")
    val editedById: Long? = null,

    @Column(name = "edited_by_email")
    val editedByEmail: String? = null,

    @Column(name = "edited_by_name")
    val editedByName: String? = null,

    @Column(nullable = false, name = "edited_at")
    val editedAt: Instant = Instant.now(),

    @Column(name = "change_summary")
    val changeSummary: String? = null
)

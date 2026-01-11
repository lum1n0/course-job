package com.knowledge.base.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "article")
data class Article(
    @Id
    @Column(nullable = false, name = "article_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "article_seq")
    @SequenceGenerator(name = "article_seq", sequenceName = "article_sequence", allocationSize = 1)
    val id: Long = 0,

    @Column(nullable = false, name = "title")
    val title: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, name = "description", columnDefinition = "jsonb")
    val description: JsonNode? = null,

    @Column(nullable = false, name = "is_delete")
    val isDelete: Boolean = false,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    val category: Category,

    @Column(name = "video_path")
    val videoPath: List<String>?,

    @Column(name = "file_path")
    val filePath: List<String>? = null,
)
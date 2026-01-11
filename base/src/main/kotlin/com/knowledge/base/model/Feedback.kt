package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(name = "feedback")
data class Feedback(

    @Id
    @Column(nullable = false, name = "feedback_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feedback_seq")
    @SequenceGenerator(name = "feedback_seq", sequenceName = "feedback_sequence", allocationSize = 1)
    val id: Long = 0,

    @Column(nullable = false, name = "title")
    val title: String = "",

    @Column(nullable = false, name = "description")
    val description: String = "",

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne
    @JoinColumn(name = "article_id")
    val article: Article,

    @Column(nullable = false, name = "is_answered")
    val isAnswered: Boolean = false,
)

package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class Role(
    @Id
    @Column(nullable = false, name = "role_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "title")
    val title: String = "",
)

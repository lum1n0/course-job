package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(
    name = "moderator_permission",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["moderator_id", "access_role_id"])
    ]
)
data class ModeratorPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    val moderator: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_role_id", nullable = false)
    val accessRole: AccessRole,

    @Column(nullable = false, name = "enabled")
    val enabled: Boolean = true
)

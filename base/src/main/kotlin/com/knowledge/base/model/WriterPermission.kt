package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(
    name = "writer_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["writer_id", "access_role_id"])]
)
data class WriterPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "writer_perm_seq")
    @SequenceGenerator(name = "writer_perm_seq", sequenceName = "writer_perm_sequence", allocationSize = 1)
    @Column(name = "writer_permission_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "writer_id", nullable = false)
val writer: User,

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "access_role_id", nullable = false)
val accessRole: AccessRole,

@Column(name = "enabled", nullable = false)
val enabled: Boolean = true
)


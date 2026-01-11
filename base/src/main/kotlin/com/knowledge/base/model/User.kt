package com.knowledge.base.model

import jakarta.persistence.*

@Table(name = "users")
@Entity
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
    @Column(nullable = false, name = "user_id")
    val id: Long = 0,

    @Column(name = "first_name")
    val firstName: String? = "",

    @Column(nullable = false, name = "last_name")
    val lastName: String = "",

    @Column(nullable = false, unique = true, name = "email")
    val email: String = "",

    @Column(name = "password")
    var password: String? = null,

    @JoinColumn(name = "role_id")
    @ManyToOne
    var role: Role,

    @Column(nullable = false, name = "is_delete")
    val isDelete: Boolean = false,

    // --- НОВОЕ ПОЛЕ ---
    // Этот флаг показывает, был ли пользователь создан из LDAP (true) или локально (false).
    @Column(name = "is_from_ldap", nullable = false)
    val isFromLdap: Boolean = false,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_access_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "access_role_id")]
    )
    var accessRoles: MutableList<AccessRole> = mutableListOf()
)

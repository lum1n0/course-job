package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @Column(nullable = false, name = "categories_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "categories_sequence", allocationSize = 1)
    val id: Long = 0,

    @Column(nullable = false, name = "description")
    val description: String = "",

    @Column(nullable = false, name = "icon_path")
    val iconPath: String = "",

    @Column(nullable = false, name = "is_delete")
    val isDelete: Boolean = false,

    // Новый родитель
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    // Дочерние
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var children: MutableList<Category> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "category_access_roles",
        joinColumns = [JoinColumn(name = "category_id")],
        inverseJoinColumns = [JoinColumn(name = "access_role_id")]
    )
    var accessRoles: MutableList<AccessRole> = mutableListOf(),

    @OneToMany(
        mappedBy = "category",
        cascade = [CascadeType.REMOVE],
        orphanRemoval = true
    )
    val articles: List<Article> = emptyList()
)

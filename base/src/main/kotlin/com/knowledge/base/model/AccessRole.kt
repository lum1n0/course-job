package com.knowledge.base.model

import jakarta.persistence.*

@Entity
@Table(
    name = "access_role",
    uniqueConstraints = [UniqueConstraint(columnNames = ["translit_title"])]
)
data class AccessRole(
    @Id
    @Column(nullable = false, name = "access_role_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_role_seq")
    @SequenceGenerator(name = "access_role_seq", sequenceName = "access_role_sequence", allocationSize = 1)
    val id: Long = 0,


    @Column(nullable = false, name = "title")
val title: String = "",

@Column(nullable = false, name = "translit_title")
val translitTitle: String = "",

@ManyToMany(mappedBy = "accessRoles")
val users: MutableList<User> = mutableListOf(),

@ManyToMany(mappedBy = "accessRoles")
val categories: MutableList<Category> = mutableListOf(),
) {
    @PreRemove
    private fun removeAssociations() {
        for (user in this.users) {
            user.accessRoles.remove(this)
        }
        for (category in this.categories) {
            category.accessRoles.remove(this)
        }
    }
}
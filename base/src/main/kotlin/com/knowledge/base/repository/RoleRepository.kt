package com.knowledge.base.repository

import com.knowledge.base.model.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository: JpaRepository<Role, Long> {
    fun findByTitle(title: String): Role?
}
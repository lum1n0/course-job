package com.knowledge.base.repository

import com.knowledge.base.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long>{
    fun findByEmail(email: String): User?
    fun findByFirstName(firstName: String): List<User>
    fun findByLastName(lastName: String): List<User>
    fun findAllByIsDeleteFalse(pageable: Pageable): Page<User>
    fun findAllByEmailIgnoreCase(email: String): List<User>
    fun findAllByIsDeleteTrue(pageable: Pageable): Page<User>
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accessRoles WHERE u.id = :userId")
    fun findByIdWithAccessRoles(@Param("userId") userId: Long): Optional<User>
    fun findAllByIsFromLdapTrue(pageable: Pageable): Page<User>
    fun findByEmailIgnoreCase(email: String): User?


}
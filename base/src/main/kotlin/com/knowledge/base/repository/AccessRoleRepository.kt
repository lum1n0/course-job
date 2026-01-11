package com.knowledge.base.repository

import com.knowledge.base.model.AccessRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AccessRoleRepository : JpaRepository<AccessRole, Long> {

    fun findByTitle(title: String): AccessRole?
    fun findByTranslitTitle(translitTitle: String): AccessRole?

    @Query(
        """
        select distinct ar
        from AccessRole ar
        left join fetch ar.categories c
        where ar.id in :ids
        """
    )
    fun findAllWithCategoriesByIds(@Param("ids") ids: Collection<Long>): List<AccessRole>
}

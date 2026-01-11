package com.knowledge.base.repository

import com.knowledge.base.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findAllByIsDeleteFalse(): List<Category>

    fun findByDescriptionContainingIgnoreCase(description: String): List<Category>

    fun findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(description: String): List<Category>

    fun findByParentIsNull(): List<Category>

    fun findAllByParentId(parentId: Long): List<Category>

    fun findAllByParentIdAndIsDeleteFalse(parentId: Long): List<Category>

    fun findAllByIsDeleteFalseAndParentIsNull(): List<Category>

    // true, если candidateId является потомком ancestorId
    @Query(
        value = """
        WITH RECURSIVE sub AS (
            SELECT c.categories_id
            FROM categories c
            WHERE c.categories_id = :ancestorId
            UNION ALL
            SELECT ch.categories_id
            FROM categories ch
            JOIN sub p ON p.categories_id = ch.parent_id
        )
        SELECT EXISTS (SELECT 1 FROM sub WHERE categories_id = :candidateId)
        """,
        nativeQuery = true
    )
    fun isDescendant(
        @Param("ancestorId") ancestorId: Long,
        @Param("candidateId") candidateId: Long
    ): Boolean
}

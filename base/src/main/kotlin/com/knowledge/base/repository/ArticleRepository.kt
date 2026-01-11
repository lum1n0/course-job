package com.knowledge.base.repository

import com.knowledge.base.model.Article
import com.knowledge.base.model.Category
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArticleRepository : JpaRepository<Article, Long> {

    // Существующие методы
    fun findAllByIsDeleteFalse(): List<Article>
    fun findAllByCategoryAndIsDeleteFalse(category: Category): List<Article>
    fun findByTitle(title: String): Article?
    // ДОБАВЬТЕ эти методы
    fun findAllByCategory(category: Category): List<Article>
    fun findByTitleContainingIgnoreCase(title: String): List<Article>
    fun findByTitleContainingIgnoreCaseAndIsDeleteFalse(title: String): List<Article>
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.category c LEFT JOIN FETCH c.accessRoles")
    fun findAllWithCategoryAndAccessRoles(): List<Article>

    @Query("""
select a from Article a
left join fetch a.category c
left join fetch c.accessRoles
where a.id = :id
""")
    fun findByIdWithCategoryAndRoles(@Param("id") id: Long): Article?


}

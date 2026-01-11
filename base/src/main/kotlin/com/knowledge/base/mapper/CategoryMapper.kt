package com.knowledge.base.mapper

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.model.Category
import org.hibernate.proxy.HibernateProxy
import org.springframework.stereotype.Component

@Component
class CategoryMapper(private val accessRoleMapper: AccessRoleMapper) {

    fun toDto(category: Category): CategoryDto {
        val parentId: Long? = category.parent?.let { parent ->
            when (parent) {
                is HibernateProxy -> parent.hibernateLazyInitializer.identifier as Long
                else -> parent.id
            }
        }

        return CategoryDto(
            id = category.id,
            description = category.description,
            iconPath = category.iconPath,
            isDelete = category.isDelete,
            parentId = parentId,
            accessRolesDto = category.accessRoles.map { accessRoleMapper.toDto(it) }
        )
    }

    // parent выставляется в сервисе по parentId
    fun toEntity(dto: CategoryDto): Category {
        return Category(
            id = dto.id,
            description = dto.description,
            iconPath = dto.iconPath,
            isDelete = dto.isDelete,
            parent = null,
            children = mutableListOf(),
            accessRoles = dto.accessRolesDto.map { accessRoleMapper.toEntity(it) }.toMutableList(),
            articles = emptyList()
        )
    }
}

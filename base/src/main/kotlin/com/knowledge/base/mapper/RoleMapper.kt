package com.knowledge.base.mapper

import com.knowledge.base.dto.RoleDto
import com.knowledge.base.model.Role
import org.springframework.stereotype.Component

@Component
class RoleMapper {
    fun toDto(role: Role?): RoleDto {
        return if (role != null) {
            RoleDto(id = role.id, title = role.title)
        } else {
            RoleDto(id = 0, title = "Не указана")
        }
    }

    fun toEntity(dto: RoleDto): Role {
        return Role(id = dto.id, title = dto.title)
    }
}
package com.knowledge.base.mapper

import com.knowledge.base.dto.AccessRoleDto
import com.knowledge.base.model.AccessRole
import org.springframework.stereotype.Component

@Component
class AccessRoleMapper {

    fun toDto(accessRole: AccessRole?): AccessRoleDto {
        return if (accessRole != null) {
            AccessRoleDto(
                id = accessRole.id,
                title = accessRole.title,
                translitTitle = accessRole.translitTitle
            )
        } else {
            AccessRoleDto(id = 0, title = "Не указана", translitTitle = "")
        }
    }

    fun toEntity(dto: AccessRoleDto): AccessRole {
        return AccessRole(
            id = dto.id,
            title = dto.title,
            translitTitle = dto.translitTitle.ifBlank { "" }
        )
    }
}
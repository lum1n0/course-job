package com.knowledge.base.mapper

import com.knowledge.base.dto.AccessRoleDto
import com.knowledge.base.dto.AuthRequest
import com.knowledge.base.dto.UserDto
import com.knowledge.base.model.AccessRole
import com.knowledge.base.model.User
import org.modelmapper.ModelMapper
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val modelMapper: ModelMapper,
    private val roleMapper: RoleMapper,
    private val accessRoleMapper: AccessRoleMapper
) {

    fun toDto(user: User): UserDto {
        val userDto = modelMapper.map(user, UserDto::class.java)
        userDto.roleDto = roleMapper.toDto(user.role)
        userDto.accessRolesDto = user.accessRoles.map { accessRoleMapper.toDto(it) }
        return userDto
    }

    fun toEntity(dto: UserDto): User {
        val user = modelMapper.map(dto, User::class.java)
        user.role = dto.roleDto?.let { roleMapper.toEntity(it) }!!
        user.accessRoles = dto.accessRolesDto.map { accessRoleMapper.toEntity(it) }.toMutableList()
        return user
    }

    fun authToEntity(dto: AuthRequest): User {
        return modelMapper.map(dto, User::class.java)
    }
}

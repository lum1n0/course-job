package com.knowledge.base.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO для создания нового пользователя с валидацией полей.
 */
data class CreateUserRequest(
    @field:Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    val firstName: String? = null,

    @field:NotBlank(message = "Фамилия не может быть пустой")
    @field:Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
    val lastName: String,

    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email (должен содержать символ @)")
    val email: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    @field:Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    val password: String,

    val roleDto: RoleDto = RoleDto(),
    val accessRolesDto: List<AccessRoleDto> = emptyList()
) {
    /**
     * Конвертирует CreateUserRequest в UserDto для сервиса.
     */
    fun toUserDto(): UserDto = UserDto(
        firstName = firstName,
        lastName = lastName,
        email = email,
        password = password,
        roleDto = roleDto,
        accessRolesDto = accessRolesDto
    )
}

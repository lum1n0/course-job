package com.knowledge.base.controller

import com.knowledge.base.dto.LdapUserDto
import com.knowledge.base.service.LdapService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ldap")
@Tag(name = "LDAP", description = "Управление пользователями LDAP/Active Directory")
class LdapController(private val ldapService: LdapService) {

    @Operation(
        summary = "Получить всех пользователей LDAP",
        description = "Возвращает список всех пользователей из LDAP/Active Directory (только для администраторов)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Список пользователей успешно получен",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = List::class)
                )]
            ),
            ApiResponse(responseCode = "403", description = "Доступ запрещен")
        ]
    )
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllLdapUsers(): ResponseEntity<List<LdapUserDto>> {
        val users = ldapService.getAllLdapUsers()
        return ResponseEntity.ok(users)
    }
}

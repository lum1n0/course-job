package com.knowledge.base.controller

import com.knowledge.base.dto.LdapUserDto
import com.knowledge.base.service.LdapService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ldap")
class LdapController(private val ldapService: LdapService) {

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllLdapUsers(): ResponseEntity<List<LdapUserDto>> {
        val users = ldapService.getAllLdapUsers()
        return ResponseEntity.ok(users)
    }
}

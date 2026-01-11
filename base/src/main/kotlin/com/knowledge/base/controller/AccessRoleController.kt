package com.knowledge.base.controller

import com.knowledge.base.dto.AccessRoleDto
import com.knowledge.base.service.AccessRoleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/access-role")
class AccessRoleController(private val accessRoleService: AccessRoleService) {

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    fun getAllAccessRole(authentication: Authentication): ResponseEntity<List<AccessRoleDto>> {
        val result = accessRoleService.getAccessRolesVisibleForUser(authentication.name)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    fun getAccessRoleByTitle(@RequestParam title: String): ResponseEntity<AccessRoleDto?> {
        val accessRole = accessRoleService.findAccessRoleByTitle(title)
        return ResponseEntity.ok(accessRole)
    }

    @GetMapping("/full/user-has-access")
    fun checkUserHasAccessRole(@RequestParam userId: Long, @RequestParam accessRoleTitle: String): ResponseEntity<Boolean> {
        val hasAccess = accessRoleService.checkUserHasAccessRole(userId, accessRoleTitle)
        return ResponseEntity.ok(hasAccess)
    }

    @PostMapping
    fun createAccessRole(@RequestBody accessRoleDto: AccessRoleDto): ResponseEntity<AccessRoleDto> {
        val newAccessRole = accessRoleService.createAccessRole(accessRoleDto)
        return ResponseEntity.ok(newAccessRole)
    }

    @PutMapping("/{id}")
    fun updateAccessRole(@PathVariable id: Long, @RequestBody accessRoleDto: AccessRoleDto): ResponseEntity<AccessRoleDto> {
        val updated = accessRoleService.updateAccessRole(id, accessRoleDto)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccessRole(@PathVariable id: Long){
        accessRoleService.deleteAccessRoleById(id)
    }
}
package com.knowledge.base.service

import com.knowledge.base.dto.ModeratorPermissionDto
import com.knowledge.base.model.Category
import com.knowledge.base.model.ModeratorPermission
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.ModeratorPermissionRepository
import com.knowledge.base.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ModeratorPermissionService(
    private val userRepository: UserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val moderatorPermissionRepository: ModeratorPermissionRepository
) {

    @Transactional
    fun grant(moderatorId: Long, accessRoleId: Long): ModeratorPermissionDto {
        val moderator = userRepository.findById(moderatorId)
            .orElseThrow { IllegalArgumentException("Moderator not found") }

        require(moderator.role.title == "MODERATOR") { "User is not MODERATOR" }

        val ar = accessRoleRepository.findById(accessRoleId)
            .orElseThrow { IllegalArgumentException("AccessRole not found") }

        if (!moderatorPermissionRepository.existsByModeratorIdAndAccessRoleIdAndEnabledTrue(
                moderatorId, accessRoleId
            )
        ) {
            val saved = moderatorPermissionRepository.save(
                ModeratorPermission(
                    moderator = moderator,
                    accessRole = ar,
                    enabled = true
                )
            )
            return ModeratorPermissionDto(saved.id, moderator.id, ar.id, true)
        }

        return ModeratorPermissionDto(0, moderator.id, ar.id, true)
    }

    @Transactional
    fun revoke(moderatorId: Long, accessRoleId: Long) {
        moderatorPermissionRepository.deleteByModeratorIdAndAccessRoleId(moderatorId, accessRoleId)
    }

    @Transactional(readOnly = true)
    fun listForModerator(moderatorId: Long): List<ModeratorPermissionDto> {
        return moderatorPermissionRepository.findAllByModeratorIdAndEnabledTrue(moderatorId)
            .map { ModeratorPermissionDto(it.id, it.moderator.id, it.accessRole.id, it.enabled) }
    }

    @Transactional(readOnly = true)
    fun checkModeratorCanEditCategory(moderatorId: Long, category: Category): Boolean {
        val categoryAccessRoleIds = category.accessRoles.map { it.id }.toSet()
        val permissions = moderatorPermissionRepository.findAllByModeratorIdAndEnabledTrue(moderatorId)
        return permissions.any { it.accessRole.id in categoryAccessRoleIds }
    }
}

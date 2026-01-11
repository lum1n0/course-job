package com.knowledge.base.repository

import com.knowledge.base.model.ModeratorPermission
import org.springframework.data.jpa.repository.JpaRepository

interface ModeratorPermissionRepository : JpaRepository<ModeratorPermission, Long> {
    fun findAllByModeratorIdAndEnabledTrue(moderatorId: Long): List<ModeratorPermission>

    fun existsByModeratorIdAndAccessRoleIdAndEnabledTrue(
        moderatorId: Long,
        accessRoleId: Long
    ): Boolean

    fun deleteByModeratorIdAndAccessRoleId(moderatorId: Long, accessRoleId: Long)
}

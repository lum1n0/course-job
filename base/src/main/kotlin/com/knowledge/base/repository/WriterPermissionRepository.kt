package com.knowledge.base.repository

import com.knowledge.base.model.WriterPermission
import org.springframework.data.jpa.repository.JpaRepository

interface WriterPermissionRepository : JpaRepository<WriterPermission, Long> {
    fun findAllByWriterIdAndEnabledTrue(writerId: Long): List<WriterPermission>
    fun existsByWriterIdAndAccessRoleIdAndEnabledTrue(writerId: Long, accessRoleId: Long): Boolean
    fun deleteByWriterIdAndAccessRoleId(writerId: Long, accessRoleId: Long): Long
}


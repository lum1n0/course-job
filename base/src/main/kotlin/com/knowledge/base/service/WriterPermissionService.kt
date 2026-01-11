package com.knowledge.base.service

import com.knowledge.base.dto.CategoryDto
import com.knowledge.base.dto.WriterPermissionDto
import com.knowledge.base.mapper.AccessRoleMapper
import com.knowledge.base.mapper.ArticleMapper
import com.knowledge.base.mapper.CategoryMapper
import com.knowledge.base.model.Category
import com.knowledge.base.model.WriterPermission
import com.knowledge.base.repository.AccessRoleRepository
import com.knowledge.base.repository.ArticleRepository
import com.knowledge.base.repository.CategoryRepository
import com.knowledge.base.repository.UserRepository
import com.knowledge.base.repository.WriterPermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class WriterPermissionService(
    private val categoryRepository: CategoryRepository,
    private val categoryMapper: CategoryMapper,
    private val accessRoleMapper: AccessRoleMapper,
    private val userRepository: UserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val articleRepository: ArticleRepository,
    private val articleMapper: ArticleMapper,
    private val writerPermissionRepository: WriterPermissionRepository
) {
    @Transactional
    fun grant(writerId: Long, accessRoleId: Long): WriterPermissionDto {
        val writer = userRepository.findById(writerId).orElseThrow { IllegalArgumentException("Writer not found") }
        require(writer.role.title == "WRITER") { "User is not WRITER" }

        val ar = accessRoleRepository.findById(accessRoleId).orElseThrow { IllegalArgumentException("AccessRole not found") }
        if (!writerPermissionRepository.existsByWriterIdAndAccessRoleIdAndEnabledTrue(writerId, accessRoleId)) {
            val saved = writerPermissionRepository.save(
                WriterPermission(
                    writer = writer,
                    accessRole = ar,
                    enabled = true
                )
            )
            return WriterPermissionDto(saved.id, writer.id, ar.id, true)
        }
        return WriterPermissionDto(0, writer.id, ar.id, true)
    }

    @Transactional
    fun revoke(writerId: Long, accessRoleId: Long) {
        writerPermissionRepository.deleteByWriterIdAndAccessRoleId(writerId, accessRoleId)
    }

    @Transactional(readOnly = true)
    fun listForWriter(writerId: Long): List<WriterPermissionDto> {
        return writerPermissionRepository.findAllByWriterIdAndEnabledTrue(writerId)
            .map { WriterPermissionDto(it.id, it.writer.id, it.accessRole.id, it.enabled) }
    }

    @Transactional(readOnly = true)
    fun checkWriterCanEditCategory(writerId: Long, category: Category): Boolean {
        val categoryAccessRoleIds = category.accessRoles.map { it.id }.toSet()
        val permissions = writerPermissionRepository.findAllByWriterIdAndEnabledTrue(writerId)
        return permissions.any { it.accessRole.id in categoryAccessRoleIds }
    }


}
